import {
  Clock,
  MapPin,
  Bell,
  Compass,
  Calendar,
  Map,
  BarChart4,
  Zap
} from 'lucide-react';

const features = [
  {
    icon: Clock,
    title: "Real-Time Tracking",
    description: "Track buses live on the map with precise arrival times."
  },
  {
    icon: MapPin,
    title: "All Bus Stops",
    description: "Find any stop in Maribor with detailed information."
  },
  {
    icon: Bell,
    title: "Delay Alerts",
    description: "Get notified about delays and schedule changes."
  },
  {
    icon: Compass,
    title: "Smart Navigation",
    description: "Find the best route to your destination."
  },
  {
    icon: Calendar,
    title: "Schedule Planner",
    description: "Plan your trips with full schedule access."
  },
  {
    icon: Zap,
    title: "Fast & Reliable",
    description: "Lightning-fast updates and accurate data."
  },
  {
    icon: Map,
    title: "Interactive Map",
    description: "Explore the entire bus network visually."
  },
  {
    icon: BarChart4,
    title: "Travel Insights",
    description: "Track your travel patterns and optimize commutes."
  }
];

const Features = () => {
  return (
    <section id="features" className="py-24 bg-white">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section header */}
        <div className="text-center max-w-2xl mx-auto mb-16">
          <span className="text-marprom-600 font-semibold text-sm uppercase tracking-wider">Features</span>
          <h2 className="text-4xl font-bold text-gray-900 mt-3 mb-4">
            Everything you need for better commutes
          </h2>
          <p className="text-lg text-gray-600">
            Powerful features designed to make public transit in Maribor simple and stress-free.
          </p>
        </div>

        {/* Features grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {features.map((feature, index) => {
            const Icon = feature.icon;
            return (
              <div
                key={index}
                className="group p-6 rounded-2xl bg-gray-50 hover:bg-white border border-transparent hover:border-gray-200 hover:shadow-xl transition-all duration-300"
              >
                <div className="w-12 h-12 bg-marprom-100 rounded-xl flex items-center justify-center mb-4 group-hover:bg-marprom-600 transition-colors duration-300">
                  <Icon className="h-6 w-6 text-marprom-600 group-hover:text-white transition-colors duration-300" />
                </div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">{feature.title}</h3>
                <p className="text-gray-600 text-sm">{feature.description}</p>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};

export default Features;
