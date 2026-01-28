import { ArrowRight, Clock, MapPin, Bus } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getApiUrl } from "../../config/api";
import { useTheme } from "../../context/ThemeContext";

const Hero = () => {
  const [busCount, setBusCount] = useState<number | null>(null);
  const { isDarkMode } = useTheme();

  useEffect(() => {
    fetch(getApiUrl("routes/active"))
      .then((res) => {
        if (!res.ok) throw new Error("Failed to fetch");
        return res.json();
      })
      .then((json) => setBusCount(json.data))
      .catch(() => setBusCount(0));
  }, []);

  return (
    <section className={`relative min-h-screen flex items-center overflow-hidden ${
      isDarkMode
        ? 'bg-gradient-to-b from-slate-900 via-slate-950 to-slate-900'
        : 'bg-gradient-to-b from-slate-50 via-white to-slate-50'
    }`}>
      {/* Background decoration */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className={`absolute -top-40 -right-40 w-96 h-96 rounded-full blur-3xl ${
          isDarkMode ? 'bg-marprom-600/10' : 'bg-marprom-600/5'
        }`}></div>
        <div className={`absolute top-1/2 -left-40 w-[500px] h-[500px] rounded-full blur-3xl ${
          isDarkMode ? 'bg-marprom-600/10' : 'bg-marprom-600/5'
        }`}></div>
        <div className={`absolute bottom-20 right-1/4 w-72 h-72 rounded-full blur-3xl ${
          isDarkMode ? 'bg-marprom-600/5' : 'bg-marprom-600/3'
        }`}></div>
      </div>

      <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-20 relative z-10">
        <div className="grid lg:grid-cols-2 gap-12 lg:gap-16 items-center">
          {/* Left content */}
          <div className="space-y-8 animate-fade-in">
            {/* Badge */}
            <div className={`inline-flex items-center gap-2 rounded-full px-4 py-2 shadow-sm ${
              isDarkMode
                ? 'bg-marprom-600/10 border border-marprom-500/20'
                : 'bg-marprom-50 border border-marprom-200/50'
            }`}>
              <span className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
              </span>
              <span className={`text-sm font-medium ${
                isDarkMode ? 'text-marprom-400' : 'text-marprom-700'
              }`}>
                Real-time bus tracking
              </span>
            </div>

            {/* Heading */}
            <h1 className={`text-4xl sm:text-5xl lg:text-6xl font-bold leading-tight tracking-tight ${
              isDarkMode ? 'text-white' : 'text-slate-900'
            }`}>
              Your Maribor
              <span className="block text-marprom-600">Bus Companion</span>
            </h1>

            {/* Description */}
            <p className={`text-lg sm:text-xl max-w-lg leading-relaxed ${
              isDarkMode ? 'text-slate-400' : 'text-slate-600'
            }`}>
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
                className={`inline-flex items-center gap-2 font-semibold px-8 py-4 rounded-xl border transition-all duration-300 shadow-sm ${
                  isDarkMode
                    ? 'bg-slate-800 text-white border-slate-700 hover:border-marprom-600 hover:text-marprom-400'
                    : 'bg-white text-slate-700 border-slate-200 hover:border-marprom-600 hover:text-marprom-600'
                }`}
              >
                Sign In
              </Link>
            </div>

            {/* Features list */}
            <div className="grid grid-cols-3 gap-4 sm:gap-6 pt-4">
              <div className="flex items-center gap-3">
                <div className={`flex items-center justify-center w-10 h-10 rounded-xl ${
                  isDarkMode ? 'bg-marprom-600/10' : 'bg-marprom-50'
                }`}>
                  <Bus className="h-5 w-5 text-marprom-600" />
                </div>
                <span className={`text-sm font-medium ${
                  isDarkMode ? 'text-slate-300' : 'text-slate-700'
                }`}>
                  Live Tracking
                </span>
              </div>
              <div className="flex items-center gap-3">
                <div className={`flex items-center justify-center w-10 h-10 rounded-xl ${
                  isDarkMode ? 'bg-marprom-600/10' : 'bg-marprom-50'
                }`}>
                  <Clock className="h-5 w-5 text-marprom-600" />
                </div>
                <span className={`text-sm font-medium ${
                  isDarkMode ? 'text-slate-300' : 'text-slate-700'
                }`}>
                  Schedules
                </span>
              </div>
              <div className="flex items-center gap-3">
                <div className={`flex items-center justify-center w-10 h-10 rounded-xl ${
                  isDarkMode ? 'bg-marprom-600/10' : 'bg-marprom-50'
                }`}>
                  <MapPin className="h-5 w-5 text-marprom-600" />
                </div>
                <span className={`text-sm font-medium ${
                  isDarkMode ? 'text-slate-300' : 'text-slate-700'
                }`}>
                  Routes
                </span>
              </div>
            </div>
          </div>

          {/* Right content - Hero image */}
          <div className="relative animate-slide-up lg:animate-slide-in-right">
            <div className="relative">
              {/* Main image container */}
              <div className={`relative rounded-3xl overflow-hidden shadow-2xl ${
                isDarkMode ? 'ring-1 ring-slate-700' : 'ring-1 ring-slate-200/50'
              }`}>
                <img
                  src="https://upload.wikimedia.org/wikipedia/commons/1/17/Marprom_Wagen_157_auf_dem_Glavni_Most.jpg"
                  alt="Maribor city bus"
                  className="w-full h-auto object-cover"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent"></div>
              </div>

              {/* Floating card - Active buses */}
              <div className={`absolute -bottom-6 -left-6 rounded-2xl shadow-xl p-4 animate-bounce-slow ${
                isDarkMode
                  ? 'bg-slate-800 ring-1 ring-slate-700'
                  : 'bg-white ring-1 ring-slate-100'
              }`}>
                <div className="flex items-center gap-3">
                  <div
                    className={`h-3 w-3 rounded-full ${
                      busCount === null
                        ? "bg-slate-400"
                        : busCount < 1
                          ? "bg-red-500"
                          : busCount < 15
                            ? "bg-amber-500"
                            : "bg-emerald-500"
                    } animate-pulse`}
                  ></div>
                  <div>
                    <p className={`text-2xl font-bold ${
                      isDarkMode ? 'text-white' : 'text-slate-900'
                    }`}>
                      {busCount ?? "..."}
                    </p>
                    <p className={`text-sm ${
                      isDarkMode ? 'text-slate-400' : 'text-slate-500'
                    }`}>buses active</p>
                  </div>
                </div>
              </div>

              {/* Floating card - Coverage */}
              <div className={`absolute -top-4 -right-4 rounded-2xl shadow-xl p-4 ${
                isDarkMode
                  ? 'bg-slate-800 ring-1 ring-slate-700'
                  : 'bg-white ring-1 ring-slate-100'
              }`}>
                <div className="text-center">
                  <p className="text-2xl font-bold text-marprom-600">42+</p>
                  <p className={`text-sm ${
                    isDarkMode ? 'text-slate-400' : 'text-slate-500'
                  }`}>bus routes</p>
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
