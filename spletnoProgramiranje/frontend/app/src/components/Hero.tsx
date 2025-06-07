import { ArrowRight, Clock, MapPin, Bus } from 'lucide-react';
import MagnetLines from './animations/MagnetLines';
import React, { useEffect, useState } from "react";

const Hero = () => {
  interface activeBuses {
    data: number;
  }

  const [busCount, setBusCount] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch("http://localhost:3000/v1/routes/active")
      .then((res) => {

        if (!res.ok) {
          throw new Error("Failed to fetch data");
        }
        return res.json();

      })
      .then((json: activeBuses) => {

        setBusCount(json.data);

      })
      .catch((err) => {

        setError(err.message);

      });
  }, []);

  if (error) {
    return <p>Error: {error}</p>;
  }
  if (busCount === null) {
    return <p>Loading...</p>;
  }




  return (
    <section className="pt-24 pb-12 md:pt-32 md:pb-20 bg-gradient-to-b from-gray-50 to-gray-100 relative">
      <div className="absolute inset-0 flex items-center justify-start pointer-events-none z-0">
        <MagnetLines
          rows={9}
          columns={9}
          containerWidth="1800px"
          containerHeight="600px"
          lineColor="tomato"
          lineWidth="0.8vmin"
          lineHeight="5vmin"
          baseAngle={0}
          style={{ opacity: 0.08 }}
        />
      </div>
      <div className="container mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="flex flex-col md:flex-row items-center justify-between gap-8">
          <div className="w-full md:w-1/2 space-y-6 animate-fade-in">
            <h1 className="text-4xl sm:text-5xl font-bold text-gray-900 leading-tight">
              Simplify Your <span className="text-mbusi-red-600">Maribor</span> Bus Journeys
            </h1>
            <p className="text-xl text-gray-600 max-w-xl">
              Real-time tracking, schedule updates, and smart navigation for Marprom bus lines in Maribor.
            </p>

            <div className="flex flex-wrap gap-4 pt-2">
              <a
                href="#get-started"
                className="inline-flex items-center justify-center bg-mbusi-red-600 text-white font-semibold px-6 py-3 rounded-lg shadow-md hover:bg-mbusi-red-700 transition-colors"
              >
                Try Web Version <ArrowRight className="ml-2 h-5 w-5" />
              </a>
              <a
                href="#learn-more"
                className="inline-flex items-center justify-center border border-mbusi-red-600 text-mbusi-red-600 font-semibold px-6 py-3 rounded-lg hover:bg-mbusi-red-50 transition-colors"
              >
                Learn More
              </a>
            </div>

            <div className="pt-6 grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="flex items-center space-x-2 text-gray-700">
                <Bus className="h-5 w-5 text-mbusi-red-600" />
                <span>Live Bus Tracking</span>
              </div>
              <div className="flex items-center space-x-2 text-gray-700">
                <Clock className="h-5 w-5 text-mbusi-red-600" />
                <span>Real-Time Schedules</span>
              </div>
              <div className="flex items-center space-x-2 text-gray-700">
                <MapPin className="h-5 w-5 text-mbusi-red-600" />
                <span>Smart Route Planning</span>
              </div>
            </div>
          </div>

          <div className="w-full md:w-1/2 animate-slide-up">
            <div className="relative">
              <div className="absolute inset-0 bg-gradient-to-r from-mbusi-red-600 to-mbusi-red-700 opacity-10 rounded-3xl transform rotate-3"></div>
              <img
                src="https://upload.wikimedia.org/wikipedia/commons/1/17/Marprom_Wagen_157_auf_dem_Glavni_Most.jpg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2"
                alt="Maribor city bus"
                className="relative z-10 rounded-2xl shadow-2xl object-cover w-full max-w-lg mx-auto"
              />
              <div className="absolute -bottom-4 -right-4 bg-white p-3 rounded-xl shadow-lg z-20">
                <div className="flex items-center space-x-2">
                  {busCount < 1 &&
                    <>
                      <div className="h-3 w-3 bg-red-500 rounded-full animate-pulse"></div>
                      <span className="text-sm font-medium">{busCount} buses active now</span>
                    </>
                  }

                  {busCount == 1 &&
                    <>
                      <div className="h-3 w-3 bg-yellow-500 rounded-full animate-pulse"></div>
                      <span className="text-sm font-medium">{busCount} bus active now</span>
                    </>
                  }

                  {busCount < 15 && busCount > 1 &&
                    <>
                      <div className="h-3 w-3 bg-yellow-500 rounded-full animate-pulse"></div>
                      <span className="text-sm font-medium">{busCount} buses active now</span>
                    </>
                  }

                  {busCount > 14 &&
                    <>
                      <div className="h-3 w-3 bg-green-500 rounded-full animate-pulse"></div>
                      <span className="text-sm font-medium">{busCount} buses active now</span>
                    </>
                  }


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