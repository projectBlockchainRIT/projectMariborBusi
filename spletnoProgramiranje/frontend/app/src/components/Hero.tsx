import React from 'react';
import { ArrowRight, Clock, MapPin, Bus } from 'lucide-react';

const Hero = () => {
  return (
    <section className="pt-24 pb-12 md:pt-32 md:pb-20 bg-gradient-to-b from-gray-50 to-gray-100">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
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
                  <div className="h-3 w-3 bg-green-500 rounded-full animate-pulse"></div>
                  <span className="text-sm font-medium">15 buses active now</span>
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