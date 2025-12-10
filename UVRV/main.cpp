#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <thread>
#include <chrono>
#include <random>
#include <cmath>

#include <curl/curl.h>
#include "json.hpp"

using json = nlohmann::json;

struct Point {
    double lat;
    double lon;
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

void sendToBackend(const std::string& route, const std::vector<Point>& points) {
    CURL* curl = curl_easy_init();
    if (!curl) return;

    json payload;
    payload["route"] = route;
    payload["timestamp"] = std::time(nullptr);

    for (const auto& p : points) {
        payload["points"].push_back({
            {"lat", p.lat},
            {"lon", p.lon}
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

    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_real_distribution<> speedDist(30.0, 50.0); // km/h
    std::normal_distribution<> offset(0.0, 0.00003);
    std::uniform_int_distribution<> usersDist(3, 8);

    double speed = speedDist(gen) * 1000.0 / 3600.0;

    for (size_t i = 1; i < path.size(); ++i) {
        double distance = haversine(path[i-1], path[i]);
        double travelTime = distance / speed;

        int users = usersDist(gen);
        std::vector<Point> cluster;

        for (int u = 0; u < users; ++u) {
            cluster.push_back({
                path[i].lat + offset(gen),
                path[i].lon + offset(gen)
            });
        }

        sendToBackend(selected, cluster);

        std::this_thread::sleep_for(
            std::chrono::milliseconds(static_cast<int>(travelTime * 1000))
        );
    }

    std::cout << "Simulation finished.\n";
    return 0;
}
