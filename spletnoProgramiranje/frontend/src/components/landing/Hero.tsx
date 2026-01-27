import { ArrowRight, Clock, MapPin, Bus } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

const Hero = () => {
  const [busCount, setBusCount] = useState<number | null>(null);

  useEffect(() => {
    fetch("http://20.208.138.248:8080/v1/routes/active")
      .then((res) => {
        if (!res.ok) throw new Error("Failed to fetch");
        return res.json();
      })
      .then((json) => setBusCount(json.data))
      .catch(() => setBusCount(0)); // Default to 0 if fetch fails
  }, []);

  return (
    <section className="relative min-h-screen flex items-center bg-gradient-to-br from-gray-50 via-white to-gray-100 overflow-hidden">
      {/* Background decoration */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-marprom-600/5 rounded-full blur-3xl"></div>
        <div className="absolute top-1/2 -left-40 w-96 h-96 bg-marprom-600/5 rounded-full blur-3xl"></div>
        <div className="absolute bottom-0 right-1/4 w-64 h-64 bg-marprom-600/3 rounded-full blur-3xl"></div>
      </div>

      <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-20 relative z-10">
        <div className="grid lg:grid-cols-2 gap-12 items-center">
          {/* Left content */}
          <div className="space-y-8 animate-fade-in">
            {/* Badge */}
            <div className="inline-flex items-center gap-2 bg-marprom-50 border border-marprom-200 rounded-full px-4 py-2">
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-marprom-500 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-marprom-600"></span>
              </span>
              <span className="text-marprom-700 text-sm font-medium">
                Real-time bus tracking
              </span>
            </div>

            {/* Heading */}
            <h1 className="text-5xl lg:text-6xl font-bold text-gray-900 leading-tight">
              Your Maribor
              <span className="block text-marprom-600">Bus Companion</span>
            </h1>

            {/* Description */}
            <p className="text-xl text-gray-600 max-w-lg leading-relaxed">
              Track buses in real-time, check schedules, and navigate Maribor's
              public transit with ease. Never miss your bus again.
            </p>

            {/* CTA Buttons */}
            <div className="flex flex-wrap gap-4">
              <Link
                to="/register"
                className="inline-flex items-center gap-2 bg-marprom-600 text-white font-semibold px-8 py-4 rounded-xl shadow-lg shadow-marprom-600/25 hover:bg-marprom-700 hover:shadow-xl hover:shadow-marprom-600/30 transition-all duration-300 hover:-translate-y-0.5"
              >
                Get Started Free
                <ArrowRight className="h-5 w-5" />
              </Link>
              <Link
                to="/login"
                className="inline-flex items-center gap-2 bg-white text-gray-700 font-semibold px-8 py-4 rounded-xl border-2 border-gray-200 hover:border-marprom-600 hover:text-marprom-600 transition-all duration-300"
              >
                Sign In
              </Link>
            </div>

            {/* Features list */}
            <div className="grid grid-cols-3 gap-6 pt-4">
              <div className="flex items-center gap-3">
                <div className="flex items-center justify-center w-10 h-10 bg-marprom-50 rounded-lg">
                  <Bus className="h-5 w-5 text-marprom-600" />
                </div>
                <span className="text-sm font-medium text-gray-700">
                  Live Tracking
                </span>
              </div>
              <div className="flex items-center gap-3">
                <div className="flex items-center justify-center w-10 h-10 bg-marprom-50 rounded-lg">
                  <Clock className="h-5 w-5 text-marprom-600" />
                </div>
                <span className="text-sm font-medium text-gray-700">
                  Schedules
                </span>
              </div>
              <div className="flex items-center gap-3">
                <div className="flex items-center justify-center w-10 h-10 bg-marprom-50 rounded-lg">
                  <MapPin className="h-5 w-5 text-marprom-600" />
                </div>
                <span className="text-sm font-medium text-gray-700">
                  Routes
                </span>
              </div>
            </div>
          </div>

          {/* Right content - Hero image */}
          <div className="relative animate-slide-up lg:animate-slide-in-right">
            <div className="relative">
              {/* Main image container */}
              <div className="relative rounded-3xl overflow-hidden shadow-2xl">
                <img
                  src="https://upload.wikimedia.org/wikipedia/commons/1/17/Marprom_Wagen_157_auf_dem_Glavni_Most.jpg"
                  alt="Maribor city bus"
                  className="w-full h-auto object-cover"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent"></div>
              </div>

              {/* Floating card - Active buses */}
              <div className="absolute -bottom-6 -left-6 bg-white rounded-2xl shadow-xl p-4 animate-bounce-slow">
                <div className="flex items-center gap-3">
                  <div
                    className={`h-3 w-3 rounded-full ${
                      busCount === null
                        ? "bg-gray-400"
                        : busCount < 1
                          ? "bg-red-500"
                          : busCount < 15
                            ? "bg-yellow-500"
                            : "bg-green-500"
                    } animate-pulse`}
                  ></div>
                  <div>
                    <p className="text-2xl font-bold text-gray-900">
                      {busCount ?? "..."}
                    </p>
                    <p className="text-sm text-gray-500">buses active</p>
                  </div>
                </div>
              </div>

              {/* Floating card - Coverage */}
              <div className="absolute -top-4 -right-4 bg-white rounded-2xl shadow-xl p-4">
                <div className="text-center">
                  <p className="text-2xl font-bold text-marprom-600">42+</p>
                  <p className="text-sm text-gray-500">bus routes</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default Hero;
