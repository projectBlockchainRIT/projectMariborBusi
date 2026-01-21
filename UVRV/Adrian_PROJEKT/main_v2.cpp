#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <thread>
#include <chrono>
#include <random>
#include <cmath>
#include <iomanip>
#include <sstream>
#include <filesystem>

#include "json.hpp"

using json = nlohmann::json;
namespace fs = std::filesystem;

struct Point {
    double lat;
    double lon;
};

struct UserLocation {
    int userId;
    double lat;
    double lon;
    long long timestamp;
    float signalStrength;
    float accuracy;
    std::string userType; // "on_bus", "waiting_at_station", "pedestrian", "nearby"
};

struct BusStation {
    double lat;
    double lon;
    int stationId;
    std::string name;
};

double deg2rad(double deg) {
    return deg * M_PI / 180.0;
}

double haversine(const Point& a, const Point& b) {
    constexpr double R = 6371000.0;
    double dLat = deg2rad(b.lat - a.lat);
    double dLon = deg2rad(b.lon - a.lon);
    double lat1 = deg2rad(a.lat);
    double lat2 = deg2rad(b.lat);

    double h = sin(dLat/2)*sin(dLat/2) +
               cos(lat1)*cos(lat2)*sin(dLon/2)*sin(dLon/2);
    return 2 * R * asin(sqrt(h));
}

Point generateRealisticOffset(const Point& centerPoint, std::mt19937& gen, double spreadMeters) {
    std::uniform_real_distribution<> angleDist(0.0, 2 * M_PI);
    // Rayleigh-like distribution (gamma(2, 0.25)) scaled by spread
    // Clipped to 2.5 sigma to avoid extreme outliers
    std::gamma_distribution<> radiusDist(2.0, 0.25);

    double angle = angleDist(gen);
    double r = radiusDist(gen);
    r = std::min(r, 2.5);  // Clip to reasonable range
    double radius = r * spreadMeters;

    double latOffset = (radius / 111000.0) * cos(angle);
    double lonOffset = (radius / (111000.0 * cos(deg2rad(centerPoint.lat)))) * sin(angle);

    return {centerPoint.lat + latOffset, centerPoint.lon + lonOffset};
}

float generateSignalStrength(const Point& location, std::mt19937& gen) {
    std::normal_distribution<> quality(75.0, 15.0);
    float signal = std::max(20.0, std::min(100.0, quality(gen)));
    return signal;
}

float signalToAccuracy(float signalStrength) {
    if (signalStrength > 90) return 5.0f;
    if (signalStrength > 75) return 10.0f;
    if (signalStrength > 60) return 20.0f;
    if (signalStrength > 45) return 35.0f;
    return 50.0f;
}

// Progress bar funkcija s prikazom zapisov in ETA
void printProgressBar(size_t current, size_t total, int totalRecords = 0,
                      long long startTime = 0, int barWidth = 50) {
    float progress = static_cast<float>(current) / total;
    int pos = static_cast<int>(barWidth * progress);

    std::cout << "\r[";
    for (int i = 0; i < barWidth; ++i) {
        if (i < pos) std::cout << "█";
        else if (i == pos) std::cout << "▓";
        else std::cout << "░";
    }
    std::cout << "] " << int(progress * 100.0) << "% ("
              << current << "/" << total << ")";

    if (totalRecords > 0) {
        std::cout << " | 📝 " << totalRecords << " zapisov";
    }

    // ETA prikaz
    if (startTime > 0 && current > 0 && progress > 0.01) {
        long long elapsed = std::time(nullptr) - startTime;
        long long eta = static_cast<long long>(elapsed / progress) - elapsed;
        if (eta > 0 && eta < 3600) { // Samo če je manj kot 1 ura
            std::cout << " | ⏱️  ETA: " << eta << "s";
        }
    }

    std::cout << "      ";  // Dodatni space za brisanje starih znakov
    std::cout.flush();
}

void saveToCSV(const std::string& route, const std::vector<UserLocation>& locations,
               const std::string& dataFolder) {
    std::stringstream ss;
    ss << dataFolder << "/" << route << "_all_" << std::time(nullptr) << ".csv";
    std::string filename = ss.str();
    
    std::ofstream file(filename);
    file << "user_id,route,lat,lon,timestamp,signal_strength,accuracy_meters,user_type\n";
    
    for (const auto& loc : locations) {
        file << loc.userId << ","
             << route << ","
             << std::fixed << std::setprecision(6) << loc.lat << ","
             << std::fixed << std::setprecision(6) << loc.lon << ","
             << loc.timestamp << ","
             << std::fixed << std::setprecision(2) << loc.signalStrength << ","
             << std::fixed << std::setprecision(2) << loc.accuracy << ","
             << loc.userType << "\n";
    }
    file.close();
}

int main() {
    std::string dataFolder = "data";
    if (!fs::exists(dataFolder)) {
        fs::create_directory(dataFolder);
        std::cout << "Ustvarjen folder: " << dataFolder << "\n";
    }

    std::ifstream file("routes.json");
    json routes;
    file >> routes;

    std::cout << "Available routes:\n";
    for (const auto& r : routes) {
        std::cout << "- " << r["route"] << "\n";
    }

    std::string selected;
    std::cout << "\nSelect route: ";
    std::cin >> selected;

    json chosen;
    for (const auto& r : routes) {
        if (r["route"] == selected) {
            chosen = r;
            break;
        }
    }

    if (chosen.is_null()) {
        std::cerr << "Route not found.\n";
        return 1;
    }

    std::vector<Point> path;
    for (const auto& p : chosen["path"]) {
        path.push_back({p[0], p[1]});
    }

    // Generiraj avtobusne postaje - vsakih 12% poti je ena postaja
    std::vector<BusStation> stations;
    int stationInterval = std::max(1, (int)path.size() / 8); // 8 postaj
    for (size_t i = 0; i < path.size(); i += stationInterval) {
        stations.push_back({path[i].lat, path[i].lon, (int)(i / stationInterval), 
                           "Station_" + std::to_string(i / stationInterval)});
    }

    std::random_device rd;
    std::mt19937 gen(rd());

    // MULTI-BUS CONFIGURATION
    const int NUM_BUSES = 3;  // 3 avtobusi na liniji
    std::vector<double> busPositions(NUM_BUSES);
    std::vector<double> busSpeeds(NUM_BUSES);
    std::vector<int> busDirections(NUM_BUSES);  // +1 = naprej, -1 = nazaj

    // Initialize bus positions - spread across route (0-80%)
    // Alternating directions so they meet
    for (int i = 0; i < NUM_BUSES; ++i) {
        std::uniform_real_distribution<> speedDist(30.0, 50.0);
        busSpeeds[i] = speedDist(gen) * 1000.0 / 3600.0;

        // Alternating directions
        if (i % 2 == 0) {
            busDirections[i] = 1;  // Forward
            busPositions[i] = path.size() * 0.2 * i;
        } else {
            busDirections[i] = -1;  // Backward
            busPositions[i] = path.size() * 0.8 - i * path.size() * 0.2;
        }
    }

    std::uniform_int_distribution<> onBusUsersDist(6, 15); // Povečano: 6-15 uporabnikov v avtobusu
    std::uniform_int_distribution<> waitingUsersDist(1, 8); // Povečano: 1-8 uporabnikov na postajah
    std::uniform_int_distribution<> pedestriansDist(1, 10); // Povečano: 1-10 pesci v blizini
    std::uniform_int_distribution<> nearbyUsersDist(1, 7); // Povečano: 1-7 uporabniki v blizini
    std::uniform_int_distribution<> measurementPoints(3, 6);
    std::uniform_real_distribution<> probDist(0.0, 1.0);

    // Razponi za razprsenost (v metrih) - povečano za bolj realistično
    std::uniform_real_distribution<> onBusSpreadDist(40.0, 100.0); // Povečano iz 20-50
    std::uniform_real_distribution<> pedestrianSpreadDist(120.0, 350.0); // Povečano iz 50-150
    std::uniform_real_distribution<> nearbySpreadDist(180.0, 450.0); // Povečano iz 80-200
    
    std::cout << "\n=== Realistična simulacija linije: " << selected << " ===\n";
    std::cout << "Število avtobusov: " << NUM_BUSES << "\n";
    for (int i = 0; i < NUM_BUSES; ++i) {
        std::cout << "  Bus " << i << " hitrost: " << (busSpeeds[i] * 3.6) << " km/h\n";
    }
    std::cout << "Avtobusne postaje: " << stations.size() << "\n";
    std::cout << "Točk na liniji: " << path.size() << "\n";
    std::cout << "Segmentov za simulacijo: " << (path.size() - 1) << "\n";
    std::cout << "Podatki se shranjujejo v: " << dataFolder << "/\n\n";

    std::cout << "Začenjam simulacijo...\n";

    long long startTime = std::time(nullptr);
    std::string csvFilename = dataFolder + "/" + selected + "_multibus_" + std::to_string(startTime) + ".csv";
    std::ofstream csvFile(csvFilename);
    csvFile << "user_id,route,lat,lon,timestamp,signal_strength,accuracy_meters,user_type,bus_lat,bus_lon,bus_id\n";

    int totalRecords = 0;
    long long syntheticTime = startTime;  // Uporabi sintetični čas za unikatne timestampe

    for (size_t i = 1; i < path.size(); ++i) {
        int measurePoints_count = measurementPoints(gen);

        // Progress bar z številom zapisov in ETA
        printProgressBar(i, path.size() - 1, totalRecords, startTime);

        // Simuliraj več meritev skozi segment
        for (int m = 0; m < measurePoints_count; ++m) {
            std::vector<UserLocation> locations;
            long long currentTime = syntheticTime++;  // Povečaj za vsako meritev

            // 🚌🚌🚌 MULTIPLE BUSES
            for (int busId = 0; busId < NUM_BUSES; ++busId) {
                // Premakni vsak avtobus s svojo hitrostjo in smerjo
                std::uniform_real_distribution<> speedVariation(0.8, 2.5);
                double speed = speedVariation(gen);
                busPositions[busId] += speed * busDirections[busId];

                // Preveri meje in obrni smer (TAM IN NAZAJ)
                if (busPositions[busId] >= path.size() - 1) {
                    // Prisel do konca - obrni nazaj
                    busPositions[busId] = path.size() - 1;
                    busDirections[busId] = -1;
                } else if (busPositions[busId] <= 0) {
                    // Prisel do zacetka - obrni naprej
                    busPositions[busId] = 0;
                    busDirections[busId] = 1;
                }

                // Trenutna pozicija avtobusa
                int pos_idx = static_cast<int>(busPositions[busId]) % path.size();
                Point busCurrent = path[pos_idx];

                // 🚌 UPORABNIKI V AVTOBUSU (glavna gruca)
                int onBusCount = onBusUsersDist(gen);
                double busSpread = onBusSpreadDist(gen);
                for (int u = 0; u < onBusCount; ++u) {
                    // Slightly more variability (60-130%)
                    std::uniform_real_distribution<> userSpreadDist(0.6, 1.3);
                    double userSpread = busSpread * userSpreadDist(gen);
                    Point userPos = generateRealisticOffset(busCurrent, gen, userSpread);
                    float signal = generateSignalStrength(userPos, gen);
                    // More variable signal
                    std::normal_distribution<> signalVariation(0.0, 20.0);
                    signal = std::min(100.0f, std::max(20.0f, signal + static_cast<float>(signalVariation(gen))));
                    float accuracy = signalToAccuracy(signal);

                    locations.push_back({
                        1000 + busId * 100 + u,
                        userPos.lat,
                        userPos.lon,
                        currentTime,
                        signal,
                        accuracy,
                        "on_bus"
                    });
                }
            }

            // 🚶 PESCI V BLIZINI LINIJE (random along route)
            int pedestrianCount = pedestriansDist(gen);
            for (int p = 0; p < pedestrianCount; ++p) {
                std::uniform_int_distribution<> pedPosDist(0, path.size() - 1);
                int pedPos = pedPosDist(gen);
                Point pedLocation = path[pedPos];
                double pedSpread = pedestrianSpreadDist(gen);
                Point userPos = generateRealisticOffset(pedLocation, gen, pedSpread);
                float signal = generateSignalStrength(userPos, gen) - 10.0f;
                signal = std::max(20.0f, signal);
                float accuracy = signalToAccuracy(signal);

                locations.push_back({
                    3000 + p,
                    userPos.lat,
                    userPos.lon,
                    currentTime,
                    signal,
                    accuracy,
                    "pedestrian"
                });
            }

            // 🏠 UPORABNIKI NA POSTAJAH (gruce)
            for (const auto& station : stations) {
                if (probDist(gen) < 0.4) { // 40% chance of users at station
                    int waitingCount = waitingUsersDist(gen);
                    std::uniform_real_distribution<> stationSpreadDist(25.0, 60.0);
                    double stationSpread = stationSpreadDist(gen);
                    for (int w = 0; w < waitingCount; ++w) {
                        Point userPos = generateRealisticOffset({station.lat, station.lon}, gen, stationSpread);
                        float signal = generateSignalStrength(userPos, gen);
                        float accuracy = signalToAccuracy(signal);

                        locations.push_back({
                            2000 + station.stationId * 100 + w,
                            userPos.lat,
                            userPos.lon,
                            currentTime,
                            signal,
                            accuracy,
                            "waiting_at_station"
                        });
                    }
                }
            }

            // 🏢 BLIZNJI UPORABNIKI (v avtih, zgradbah ob poti)
            int nearbyCount = nearbyUsersDist(gen);
            for (int n = 0; n < nearbyCount; ++n) {
                std::uniform_int_distribution<> nearbyPosDist(0, path.size() - 1);
                int nearbyPos = nearbyPosDist(gen);
                Point nearbyLocation = path[nearbyPos];
                double nearbySpread = nearbySpreadDist(gen);
                Point userPos = generateRealisticOffset(nearbyLocation, gen, nearbySpread);
                float signal = generateSignalStrength(userPos, gen) - 15.0f;
                signal = std::max(20.0f, signal);
                float accuracy = signalToAccuracy(signal);

                locations.push_back({
                    4000 + n,
                    userPos.lat,
                    userPos.lon,
                    currentTime,
                    signal,
                    accuracy,
                    "nearby"
                });
            }

            // Shrani podatke v CSV
            if (!locations.empty()) {
                for (const auto& loc : locations) {
                    // Determine which bus this user belongs to (or -1 for non-bus users)
                    int userBusId = -1;
                    Point userBusLocation = {0.0, 0.0};

                    if (loc.userType == "on_bus") {
                        // Extract bus ID from user_id (1000-1099 = bus 0, 1100-1199 = bus 1, etc.)
                        userBusId = (loc.userId - 1000) / 100;
                        if (userBusId >= 0 && userBusId < NUM_BUSES) {
                            int pos_idx = static_cast<int>(busPositions[userBusId]) % path.size();
                            userBusLocation = path[pos_idx];
                        }
                    }

                    csvFile << loc.userId << ","
                             << selected << ","
                             << std::fixed << std::setprecision(6) << loc.lat << ","
                             << std::fixed << std::setprecision(6) << loc.lon << ","
                             << loc.timestamp << ","
                             << std::fixed << std::setprecision(2) << loc.signalStrength << ","
                             << std::fixed << std::setprecision(2) << loc.accuracy << ","
                             << loc.userType << ","
                             << std::fixed << std::setprecision(6) << userBusLocation.lat << ","
                             << std::fixed << std::setprecision(6) << userBusLocation.lon << ","
                             << userBusId << "\n";
                    totalRecords++;
                }

                // Posodobi progress bar po pisanju
                printProgressBar(i, path.size() - 1, totalRecords, startTime);
            }

            // Čakaj malo med meritvami (zakomentirano za hitro testiranje)
            // std::this_thread::sleep_for(
            //     std::chrono::milliseconds(static_cast<int>(travelTime * 1000 / measurePoints_count * 0.5))
            // );
        }

        // Čakaj med segmenti (zakomentirano za hitro testiranje)
        // std::this_thread::sleep_for(
        //     std::chrono::milliseconds(static_cast<int>(travelTime * 1000 * 0.2))
        // );
    }

    csvFile.close();

    std::cout << "\n\n=== ✅ Simulacija zaključena ===\n";
    std::cout << "⏱️  Čas simulacije: "
              << (std::time(nullptr) - startTime) << " sekund\n";
    std::cout << "📊 Skupaj zapisov: " << totalRecords << "\n";
    std::cout << "📁 CSV datoteka: " << csvFilename << "\n";
    std::cout << "💾 Velikost: ";

    // Izračunaj velikost datoteke
    std::ifstream checkFile(csvFilename, std::ios::binary | std::ios::ate);
    if (checkFile.is_open()) {
        auto fileSize = checkFile.tellg();
        checkFile.close();
        if (fileSize > 1024 * 1024) {
            std::cout << std::fixed << std::setprecision(2)
                      << (fileSize / 1024.0 / 1024.0) << " MB\n";
        } else {
            std::cout << std::fixed << std::setprecision(2)
                      << (fileSize / 1024.0) << " KB\n";
        }
    }

    std::cout << "\n";
    return 0;
}
