import { 
  Clock, 
  MapPin, 
  Bell, 
  Compass, 
  Calendar, 
  CreditCard, 
  Map, 
  BarChart4 
} from 'lucide-react';

const features = [
  {
    icon: <Clock className="h-6 w-6 text-mbusi-red-600" />,
    title: "Real-Time Tracking",
    description: "Track buses in real-time to never miss a ride and minimize waiting times."
  },
  {
    icon: <MapPin className="h-6 w-6 text-mbusi-red-600" />,
    title: "All Bus Stops",
    description: "Explore all Maribor bus stops with detailed information and accessibility features."
  },
  {
    icon: <Bell className="h-6 w-6 text-mbusi-red-600" />,
    title: "Journey Alerts",
    description: "Receive notifications about delays, route changes, and service disruptions."
  },
  {
    icon: <Compass className="h-6 w-6 text-mbusi-red-600" />,
    title: "Smart Navigation",
    description: "Get personalized route recommendations based on your location and destination."
  },
  {
    icon: <Calendar className="h-6 w-6 text-mbusi-red-600" />,
    title: "Schedule Planner",
    description: "Plan your journeys ahead of time with our comprehensive schedule system."
  },
  {
    icon: <CreditCard className="h-6 w-6 text-mbusi-red-600" />,
    title: "Digital Tickets",
    description: "Purchase and store bus tickets digitally for convenient, contactless travel."
  },
  {
    icon: <Map className="h-6 w-6 text-mbusi-red-600" />,
    title: "Interactive Map",
    description: "Explore the entire Maribor bus network with our interactive city map."
  },
  {
    icon: <BarChart4 className="h-6 w-6 text-mbusi-red-600" />,
    title: "Travel Insights",
    description: "Get insights about your travel habits and optimize your daily commute."
  }
];

const Features = () => {
  return (
    <section id="features" className="py-16 md:py-24 bg-white">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">Why Choose M-busi?</h2>
          <p className="text-lg text-gray-600">
            Our smart bus application offers a comprehensive set of features designed to make your daily commute in Maribor seamless and stress-free.
          </p>
        </div>
        
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
          {features.map((feature, index) => (
            <div 
              key={index} 
              className="bg-gray-50 p-6 rounded-xl shadow-sm hover:shadow-md transition-shadow border border-gray-100"
            >
              <div className="mb-4 bg-mbusi-red-50 p-3 rounded-lg inline-block">
                {feature.icon}
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">{feature.title}</h3>
              <p className="text-gray-600">{feature.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default Features;