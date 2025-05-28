import { Map, Users, Clock, Shield } from 'lucide-react';

const stats = [
  { 
    icon: <Map className="h-6 w-6 text-mbusi-red-600" />,
    value: "42",
    label: "Bus Routes", 
  },
  { 
    icon: <Users className="h-6 w-6 text-mbusi-red-600" />,
    value: "25,000+",
    label: "Daily Riders", 
  },
  { 
    icon: <Clock className="h-6 w-6 text-mbusi-red-600" />,
    value: "99.4%",
    label: "On-time Performance", 
  },
  { 
    icon: <Shield className="h-6 w-6 text-mbusi-red-600" />,
    value: "24/7",
    label: "Customer Support", 
  },
];

const About = () => {
  return (
    <section id="about" className="py-16 md:py-24 bg-gray-50">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          <div>
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-6">About Marprom Bus Service</h2>
            <p className="text-lg text-gray-600 mb-6">
              Marprom is the leading public transport provider in Maribor, committed to connecting the city with safe, reliable, and efficient bus services since 1989.
            </p>
            <p className="text-lg text-gray-600 mb-8">
              With M-busi, we're taking our service to the next level by integrating smart technology with our extensive bus network. Our goal is to make public transportation the preferred choice for everyone in Maribor.
            </p>
            
            <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
              {stats.map((stat, index) => (
                <div key={index} className="text-center">
                  <div className="flex justify-center mb-2">
                    {stat.icon}
                  </div>
                  <div className="text-2xl md:text-3xl font-bold text-mbusi-red-600">
                    {stat.value}
                  </div>
                  <div className="text-sm text-gray-500">
                    {stat.label}
                  </div>
                </div>
              ))}
            </div>
          </div>
          
          <div className="relative">
            <div className="absolute inset-0 bg-mbusi-red-600 rounded-3xl transform -rotate-3 opacity-10"></div>
            <div className="relative bg-white p-6 rounded-2xl shadow-md">
              <div className="aspect-w-16 aspect-h-9 mb-6">
                <img 
                  src="https://www.marprom.si/wp-content/uploads/Mestni-avtobusi_1024x680.jpg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2" 
                  alt="Marprom Bus Station" 
                  className="rounded-xl object-cover w-full h-64"
                />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-2">Our Mission</h3>
              <p className="text-gray-600 mb-4">
                To provide Maribor residents and visitors with a sustainable, accessible, and enjoyable public transportation experience through innovation and customer-focused service.
              </p>
              <div className="flex space-x-4">
                <img 
                  src="https://www.marprom.si/wp-content/uploads/POlni-vozni-redi_1_800x539.jpg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2" 
                  alt="Maribor City" 
                  className="w-1/2 h-32 rounded-lg object-cover"
                />
                <img 
                  src="https://www.marprom.si/wp-content/uploads/Avtobus_linija-6_1024x587.jpg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2" 
                  alt="Marprom Bus" 
                  className="w-1/2 h-32 rounded-lg object-cover"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default About;