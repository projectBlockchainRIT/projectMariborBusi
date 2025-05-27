import React from 'react';
import { Download, Search, Navigation, Bus } from 'lucide-react';

const steps = [
  {
    icon: <Download className="h-8 w-8 text-white" />,
    title: "Access the Web App",
    description: "Open M-busi in your browser and create an account in seconds.",
    color: "bg-mbusi-red-600",
    image: "https://images.pexels.com/photos/6802042/pexels-photo-6802042.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2"
  },
  {
    icon: <Search className="h-8 w-8 text-white" />,
    title: "Find Your Route",
    description: "Enter your destination or browse the interactive map to find your route.",
    color: "bg-mbusi-red-700",
    image: "https://images.pexels.com/photos/7605945/pexels-photo-7605945.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2"
  },
  {
    icon: <Navigation className="h-8 w-8 text-white" />,
    title: "Track in Real-Time",
    description: "See buses moving in real-time and get accurate arrival predictions.",
    color: "bg-mbusi-red-800",
    image: "https://images.pexels.com/photos/7605201/pexels-photo-7605201.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2"
  },
  {
    icon: <Bus className="h-8 w-8 text-white" />,
    title: "Enjoy Your Ride",
    description: "Hop on your bus with confidence and enjoy a stress-free journey.",
    color: "bg-mbusi-red-900",
    image: "https://images.pexels.com/photos/6802049/pexels-photo-6802049.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2"
  }
];

const HowItWorks = () => {
  return (
    <section id="how-it-works\" className="py-16 md:py-24 bg-gray-50">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">How M-busi Works</h2>
          <p className="text-lg text-gray-600">
            Getting started with M-busi is easy. Follow these simple steps to transform your daily commute in Maribor.
          </p>
        </div>
        
        <div className="relative">
          {/* Connection line */}
          <div className="hidden md:block absolute left-1/2 top-0 bottom-0 w-1 bg-gray-200 transform -translate-x-1/2 z-0"></div>
          
          <div className="space-y-12 relative z-10">
            {steps.map((step, index) => (
              <div key={index} className={`flex flex-col md:flex-row items-center ${index % 2 === 1 ? 'md:flex-row-reverse' : ''}`}>
                <div className="md:w-1/2 p-6">
                  <div className="flex items-center mb-4">
                    <div className={`${step.color} rounded-full p-3 mr-4 shadow-lg`}>
                      {step.icon}
                    </div>
                    <h3 className="text-2xl font-semibold text-gray-900">{step.title}</h3>
                  </div>
                  <p className="text-lg text-gray-600 ml-16">{step.description}</p>
                </div>
                
                <div className="md:w-1/2 p-6 flex justify-center">
                  <div className={`w-64 h-56 ${index % 2 === 0 ? 'bg-gradient-to-br' : 'bg-gradient-to-tr'} from-mbusi-red-500/10 to-mbusi-red-600/20 rounded-2xl flex items-center justify-center`}>
                    <img 
                      src={step.image}
                      alt={`Step ${index + 1} illustration`}
                      className="max-w-full max-h-full object-cover rounded-xl shadow-md transform rotate-3 hover:rotate-0 transition-transform"
                    />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
};

export default HowItWorks;