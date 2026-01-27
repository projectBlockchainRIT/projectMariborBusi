import { Map, Users, Clock, Shield } from 'lucide-react';
import CountUp from '../animations/CountUp';

const stats = [
  { icon: Map, value: 42, label: "Bus Routes" },
  { icon: Users, value: 25000, suffix: "+", label: "Daily Riders" },
  { icon: Clock, value: 99, suffix: "%", label: "Uptime" },
  { icon: Shield, value: 24, suffix: "/7", label: "Support" },
];

const About = () => {
  return (
    <section id="about" className="py-24 bg-gray-50">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid lg:grid-cols-2 gap-16 items-center">
          {/* Left content */}
          <div>
            <span className="text-marprom-600 font-semibold text-sm uppercase tracking-wider">About Us</span>
            <h2 className="text-4xl font-bold text-gray-900 mt-3 mb-6">
              Powered by Marprom
            </h2>
            <p className="text-lg text-gray-600 mb-6 leading-relaxed">
              Marprom is Maribor's leading public transport provider, connecting the city with safe and reliable bus services since 1989.
            </p>
            <p className="text-lg text-gray-600 mb-8 leading-relaxed">
              M-busi brings smart technology to our extensive bus network, making public transportation the preferred choice for everyone in Maribor.
            </p>

            {/* Stats grid */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-6">
              {stats.map((stat, index) => {
                const Icon = stat.icon;
                return (
                  <div key={index} className="text-center">
                    <div className="inline-flex items-center justify-center w-12 h-12 bg-marprom-100 rounded-xl mb-3">
                      <Icon className="h-6 w-6 text-marprom-600" />
                    </div>
                    <div className="text-2xl font-bold text-gray-900">
                      <CountUp from={0} to={stat.value} duration={2} />
                      {stat.suffix}
                    </div>
                    <p className="text-sm text-gray-500">{stat.label}</p>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Right content - Images */}
          <div className="relative">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-4">
                <img
                  src="https://www.marprom.si/wp-content/uploads/Mestni-avtobusi_1024x680.jpg"
                  alt="Marprom buses"
                  className="rounded-2xl shadow-lg w-full h-48 object-cover"
                />
                <img
                  src="https://www.marprom.si/wp-content/uploads/POlni-vozni-redi_1_800x539.jpg"
                  alt="Maribor city"
                  className="rounded-2xl shadow-lg w-full h-32 object-cover"
                />
              </div>
              <div className="space-y-4 pt-8">
                <img
                  src="https://www.marprom.si/wp-content/uploads/Avtobus_linija-6_1024x587.jpg"
                  alt="Bus line 6"
                  className="rounded-2xl shadow-lg w-full h-32 object-cover"
                />
                <div className="bg-marprom-600 rounded-2xl p-6 text-white">
                  <h3 className="text-xl font-bold mb-2">Our Mission</h3>
                  <p className="text-marprom-100 text-sm">
                    Sustainable, accessible, and enjoyable public transportation for everyone.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default About;
