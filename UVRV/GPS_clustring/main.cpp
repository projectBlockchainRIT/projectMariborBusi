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

#include <curl/curl.h>
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
    float signalStrength; // 0-100, višje je boljše
    float accuracy; // meters
    int type; // 0 = waiting at station, 1 = on bus
};

struct BusStation {
    double lat;
    double lon;
    int stationId;
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

// Generira realističen offset za uporabnike (bolj razpršeno od direktne linije)
Point generateRealisticOffset(const Point& centerPoint, std::mt19937& gen, double spreadMeters) {
    // Rayleigh distribucija za bolj realistični scatter
    std::uniform_real_distribution<> angleDist(0.0, 2 * M_PI);
    std::gamma_distribution<> radiusDist(2.0, spreadMeters / 3.0);
    
    double angle = angleDist(gen);
    double radius = radiusDist(gen);
    
    // Konverzija metrske razdalje v stopinje
    double latOffset = (radius / 111000.0) * cos(angle);
    double lonOffset = (radius / (111000.0 * cos(deg2rad(centerPoint.lat)))) * sin(angle);
    
    return {centerPoint.lat + latOffset, centerPoint.lon + lonOffset};
}

// Simulira GPS natančnost - slabša natančnost na gosto mestih/tunelov
float generateSignalStrength(const Point& location, std::mt19937& gen) {
    std::normal_distribution<> quality(75.0, 15.0);
    float signal = std::max(20.0, std::min(100.0, quality(gen)));
    return signal;
}

// Mapira signal strength na accuracy (standard deviation) v metrih
float signalToAccuracy(float signalStrength) {
    // Slabši signal = večja napaka
    if (signalStrength > 90) return 5.0f;  // Odličen signal
    if (signalStrength > 75) return 10.0f; // Dober
    if (signalStrength > 60) return 20.0f; // Zadovoljiv
    if (signalStrength > 45) return 35.0f; // Slab
    return 50.0f; // Zelo slab
}

// Shrani podatke v JSON datoteko
void saveToJSON(const std::string& route, const std::vector<UserLocation>& locations, 
                const std::string& dataFolder) {
    json data;
    data["route"] = route;
    data["timestamp"] = std::time(nullptr);
    data["location_count"] = locations.size();

    for (const auto& loc : locations) {
        data["locations"].push_back({
            {"user_id", loc.userId},
            {"lat", loc.lat},
            {"lon", loc.lon},
            {"timestamp", loc.timestamp},
            {"signal_strength", loc.signalStrength},
            {"accuracy_meters", loc.accuracy}
        });
    }

    // Ustvari filename
    std::stringstream ss;
    ss << dataFolder << "/" << route << "_" << std::time(nullptr) << ".json";
    std::string filename = ss.str();
    
    std::ofstream file(filename);
    file << data.dump(2);
    file.close();
}

// Shrani podatke v CSV datoteko
void saveToCSV(const std::string& route, const std::vector<UserLocation>& locations,
               const std::string& dataFolder) {
    std::stringstream ss;
    ss << dataFolder << "/" << route << "_" << std::time(nullptr) << ".csv";
    std::string filename = ss.str();
    
    std::ofstream file(filename);
    // Header
    file << "user_id,route,lat,lon,timestamp,signal_strength,accuracy_meters,user_type\n";
    
    // Podatki
    for (const auto& loc : locations) {
        std::string type_str = (loc.type == 0) ? "waiting" : "onbus";
        file << loc.userId << ","
             << route << ","
             << std::fixed << std::setprecision(6) << loc.lat << ","
             << std::fixed << std::setprecision(6) << loc.lon << ","
             << loc.timestamp << ","
             << std::fixed << std::setprecision(2) << loc.signalStrength << ","
             << std::fixed << std::setprecision(2) << loc.accuracy << ","
             << type_str << "\n";
    }
    file.close();
}

void sendToBackend(const std::string& route, const std::vector<UserLocation>& locations) {
    CURL* curl = curl_easy_init();
    if (!curl) return;

    json payload;
    payload["route"] = route;
    payload["timestamp"] = std::time(nullptr);
    payload["location_count"] = locations.size();

    for (const auto& loc : locations) {
        payload["locations"].push_back({
            {"user_id", loc.userId},
            {"lat", loc.lat},
            {"lon", loc.lon},
            {"timestamp", loc.timestamp},
            {"signal_strength", loc.signalStrength},
            {"accuracy_meters", loc.accuracy}
        });
    }

    std::string data = payload.dump();

    struct curl_slist* headers = nullptr;
    headers = curl_slist_append(headers, "Content-Type: application/json");

    curl_easy_setopt(curl, CURLOPT_URL, "http://localhost:8080/v1/simulation/test");
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
    curl_easy_setopt(curl, CURLOPT_POSTFIELDS, data.c_str());

    curl_easy_perform(curl);

    curl_slist_free_all(headers);
    curl_easy_cleanup(curl);
}

int main() {
    // Ustvari data folder
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

    // Generiraj avtobusne postaje iz poti (vsakih N točk)
    std::vector<BusStation> stations;
    int stationId = 0;
    int stationInterval = std::max(1, (int)path.size() / 8); // 8 postaj na liniji
    for (size_t i = 0; i < path.size(); i += stationInterval) {
        stations.push_back({path[i].lat, path[i].lon, stationId++});
    }
    
    std::cout << "\n📍 Avtobusne postaje: " << stations.size() << "\n";

    std::random_device rd;
    std::mt19937 gen(rd());
    
    // Simulacija realističnih parametrov
    std::uniform_real_distribution<> speedDist(30.0, 50.0); // km/h
    std::uniform_int_distribution<> usersDist(2, 8); // Manj uporabnikov v avtobusu
    std::uniform_int_distribution<> waitingUsersDist(1, 4); // Uporabniki na postajah
    std::uniform_real_distribution<> userSpeedVariation(0.7, 1.3); // Razlike v hitrostih
    std::uniform_int_distribution<> measurementPoints(3, 6); // Več točk na segment
    std::uniform_real_distribution<> onBusProbDist(0.0, 1.0); // Verjetnost, da je uporabnik v avtobusu
    
    double busSpeed = speedDist(gen) * 1000.0 / 3600.0; // m/s
    
    std::cout << "\n=== Simulacija linije: " << selected << " ===\n";
    std::cout << "Hitrost avtobusa: " << (busSpeed * 3.6) << " km/h\n";
    std::cout << "Skupno točk na liniji: " << path.size() << "\n";
    std::cout << "Podatki se shranjujejo v: " << dataFolder << "/\n\n";

    long long startTime = std::time(nullptr);
    
    // CSV datoteka za vse podatke
    std::string csvFilename = dataFolder + "/" + selected + "_all_" + std::to_string(startTime) + ".csv";
    std::ofstream csvFile(csvFilename);
    csvFile << "user_id,route,lat,lon,timestamp,signal_strength,accuracy_meters\n";

    for (size_t i = 1; i < path.size(); ++i) {
        double distance = haversine(path[i-1], path[i]);
        double travelTime = distance / busSpeed;
        
        int numUsers = usersDist(gen);
        int measurePoints = measurementPoints(gen);

        std::cout << "Segment " << i << "/" << path.size() << ": "
                  << numUsers << " uporabnikov\n";

        // Simuliraj več meritev skozi segment
        for (int m = 0; m < measurePoints; ++m) {
            std::vector<UserLocation> locations;
            
            // Interpolacija pozicije avtobusa med točkama
            double progress = static_cast<double>(m) / measurePoints;
            Point interpolated;
            interpolated.lat = path[i-1].lat + (path[i].lat - path[i-1].lat) * progress;
            interpolated.lon = path[i-1].lon + (path[i].lon - path[i-1].lon) * progress;

            // Generiraj uporabnike s črtos karakteristikami
            for (int u = 0; u < numUsers; ++u) {
                // Različne hitrosti uporabnikov
                double userSpeedFactor = userSpeedVariation(gen);
                
                // Različne razdalje od linije (nekateri bliže, nekateri dlje)
                std::uniform_real_distribution<> spreadDist(15.0, 80.0);
                double spreadMeters = spreadDist(gen);
                
                // Generiraj offset lokacije
                Point userLocation = generateRealisticOffset(interpolated, gen, spreadMeters);
                
                // Generiraj signal strength in accuracy
                float signal = generateSignalStrength(userLocation, gen);
                float accuracy = signalToAccuracy(signal);
                
                // Dodaj malo dodatnega šuma glede na accuracy
                std::normal_distribution<> latNoise(0.0, accuracy / 111000.0);
                std::normal_distribution<> lonNoise(0.0, accuracy / (111000.0 * cos(deg2rad(userLocation.lat))));
                
                userLocation.lat += latNoise(gen);
                userLocation.lon += lonNoise(gen);
                
                UserLocation loc;
                loc.userId = (i * 1000 + u); // Unikatni ID za vsak uporabnika
                loc.lat = userLocation.lat;
                loc.lon = userLocation.lon;
                loc.timestamp = startTime + static_cast<long long>((i * travelTime) + (m * travelTime / measurePoints));
                loc.signalStrength = signal;
                loc.accuracy = accuracy;
                
                locations.push_back(loc);
                
                // Piši v CSV
                csvFile << loc.userId << ","
                        << selected << ","
                        << std::fixed << std::setprecision(6) << loc.lat << ","
                        << std::fixed << std::setprecision(6) << loc.lon << ","
                        << loc.timestamp << ","
                        << std::fixed << std::setprecision(2) << loc.signalStrength << ","
                        << std::fixed << std::setprecision(2) << loc.accuracy << "\n";
            }

            // Pošlji podatke na backend
            if (!locations.empty()) {
                // sendToBackend(selected, locations);
                // Shrani tudi v JSON
                saveToJSON(selected, locations, dataFolder);
            }

            // Čakaj malo med meritvami
            std::this_thread::sleep_for(
                std::chrono::milliseconds(static_cast<int>(travelTime * 1000 / measurePoints * 0.5))
            );
        }

        // Čakaj med segmenti
        std::this_thread::sleep_for(
            std::chrono::milliseconds(static_cast<int>(travelTime * 1000 * 0.3))
        );
    }

    csvFile.close();

    std::cout << "\n=== Simulacija zaključena ===\n";
    std::cout << "Skupno časa simulacije: " 
              << (std::time(nullptr) - startTime) << " sekund\n";
    std::cout << "\nShranjena podatka:\n";
    std::cout << "- CSV: " << csvFilename << "\n";
    std::cout << "- JSON datoteke: " << dataFolder << "/" << selected << "_*.json\n";
    std::cout << "- Skupaj podatkov: " << csvFilename << "\n";
    return 0;
}
