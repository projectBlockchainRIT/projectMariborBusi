import React from 'react';
import { ArrowRight, Smartphone, Map, Bell } from 'lucide-react';

const CTA = () => {
  return (
    <section className="py-16 md:py-24 bg-mbusi-red-600">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col md:flex-row items-center justify-between gap-12">
          <div className="w-full md:w-1/2 text-white">
            <h2 className="text-3xl md:text-4xl font-bold mb-6">Ready to Transform Your Daily Commute?</h2>
            <p className="text-white/90 text-lg mb-8 max-w-lg">
              Join thousands of satisfied commuters in Maribor who have made their bus journeys simpler, more reliable, and stress-free with M-busi.
            </p>
            
            <div className="space-y-4 mb-8">
              <div className="flex items-center space-x-3">
                <Smartphone className="h-5 w-5 text-white" />
                <span>Mobile apps coming soon</span>
              </div>
              <div className="flex items-center space-x-3">
                <Map className="h-5 w-5 text-white" />
                <span>Covers all Marprom bus routes in Maribor</span>
              </div>
              <div className="flex items-center space-x-3">
                <Bell className="h-5 w-5 text-white" />
                <span>Real-time notifications and updates</span>
              </div>
            </div>
            
            <a 
              href="#get-started" 
              className="inline-flex items-center justify-center bg-white text-mbusi-red-600 font-semibold px-6 py-3 rounded-lg shadow-md hover:bg-gray-100 transition-colors"
            >
              Try Web Version Now <ArrowRight className="ml-2 h-5 w-5" />
            </a>
          </div>
          
          <div className="w-full md:w-1/2 relative">
            <div className="relative mx-auto w-full max-w-md">
              <div className="absolute inset-0 bg-white opacity-10 rounded-3xl transform rotate-3"></div>
              <div className="relative bg-white rounded-2xl shadow-xl overflow-hidden">
                <div className="h-6 bg-gray-200 flex items-center px-4">
                  <div className="h-2 w-2 rounded-full bg-red-500 mr-1"></div>
                  <div className="h-2 w-2 rounded-full bg-yellow-500 mr-1"></div>
                  <div className="h-2 w-2 rounded-full bg-green-500"></div>
                </div>
                <img 
                  src="https://images.pexels.com/photos/5797899/pexels-photo-5797899.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2" 
                  alt="M-busi App Preview" 
                  className="w-full h-auto"
                />
              </div>
              
              <div className="mt-6 text-center">
                <p className="text-white text-sm">Mobile apps for iOS and Android coming soon!</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default CTA;