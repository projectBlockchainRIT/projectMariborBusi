import { motion } from 'framer-motion';
import { 
  MapIcon, 
  ClockIcon, 
  UserGroupIcon, 
  LightBulbIcon,
  ChartBarIcon,
  ShieldCheckIcon
} from '@heroicons/react/24/outline';
import { useTheme } from '../context/ThemeContext';

const teamMembers = [
  {
    name: "Timotej Maučec",
    role: "Big boss in general",
    image: "https://media.istockphoto.com/id/531662275/photo/close-up-of-beaver.jpg?s=612x612&w=0&k=20&c=6z3yfzYJdXhm-n3nh3AQcNSD66lZURu7yf5SnLnxuDY=",
    bio: "Passionate about improving life through technology."
  },
  {
    name: "Adrian Cvetko",
    role: "BOss projekta",
    image: "https://citymagazine.si/en/beavers-are-the-cutest-animals-in-the-world/cute-baby-beavers-73-570663022bbf8__605/",
    bio: "Expert in real-time data processing and mobile app development."
  },
  {
    name: "Blaž Kolman",
    role: "Embalažonja BOSS",
    image: "https://i.ytimg.com/vi/gdwDeJpweiU/hq720.jpg?sqp=-oaymwEhCK4FEIIDSFryq4qpAxMIARUAAAAAGAElAADIQj0AgKJD&rs=AOn4CLDKfu-YYJnC0utw1uUuIJ6qIxEP-w",
    bio: "Creating intuitive and accessible user experiences."
  }
];

const values = [
  {
    title: "Innovation",
    description: "Constantly pushing boundaries to improve public transportation.",
    icon: LightBulbIcon,
  },
  {
    title: "Reliability",
    description: "Providing accurate and dependable information to our users.",
    icon: ShieldCheckIcon,
  },
  {
    title: "Community",
    description: "Building a better future for public transport together.",
    icon: UserGroupIcon,
  },
  {
    title: "Sustainability",
    description: "Promoting eco-friendly transportation solutions.",
    icon: ChartBarIcon,
  }
];

export default function AboutPage() {
  const { isDarkMode } = useTheme();
  
  return (
    <div className={`${isDarkMode ? 'bg-slate-900 text-white' : 'bg-white text-gray-900'}`}>
      {/* Hero Section */}
      <section className="relative overflow-hidden bg-blue-600">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 md:py-32">
          <div className="text-center">
            <motion.h1 
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5 }}
              className="text-4xl md:text-6xl font-bold text-white mb-6"
            >
              About m-busi
            </motion.h1>
            <motion.p 
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.2 }}
              className="text-xl text-blue-100 mb-8 max-w-3xl mx-auto"
            >
              Transforming public transportation in Maribor through innovation and technology
            </motion.p>
          </div>
        </div>
      </section>

      {/* Mission Section */}
      <section className={`py-20 ${isDarkMode ? 'bg-slate-900' : 'bg-white'}`}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.5 }}
            >
              <h2 className={`text-3xl font-bold mb-6 ${isDarkMode ? 'text-white' : 'text-gray-900'}`}>
                Our Mission
              </h2>
              <p className={`text-lg mb-6 ${isDarkMode ? 'text-gray-300' : 'text-gray-600'}`}>
                At m-busi, we're dedicated to revolutionizing public transportation in Maribor. 
                Our mission is to make public transport more accessible, efficient, and user-friendly 
                through innovative technology solutions.
              </p>
              <p className={`text-lg ${isDarkMode ? 'text-gray-300' : 'text-gray-600'}`}>
                We believe that reliable public transportation is essential for a sustainable and 
                connected city. By providing real-time information and smart routing solutions, 
                we're helping residents and visitors navigate Maribor with confidence and ease.
              </p>
            </motion.div>
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.5 }}
              className="relative h-96 rounded-xl overflow-hidden"
            >
              <img
                src="https://dynamic-media-cdn.tripadvisor.com/media/photo-o/2a/b5/2a/9c/caption.jpg?w=1200&h=-1&s=1"
                alt="Maribor bus"
                className="absolute inset-0 w-full h-full object-cover"
              />
            </motion.div>
          </div>
        </div>
      </section>

      {/* Values Section */}
      <section className={`py-20 ${isDarkMode ? 'bg-slate-800' : 'bg-gray-50'}`}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className={`text-3xl font-bold mb-4 ${isDarkMode ? 'text-white' : 'text-gray-900'}`}>
              Our Values
            </h2>
            <p className={`text-lg ${isDarkMode ? 'text-gray-300' : 'text-gray-600'}`}>
              The principles that guide our work
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
            {values.map((value, index) => (
              <motion.div
                key={value.title}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: index * 0.1 }}
                className={`p-6 rounded-xl shadow-sm ${isDarkMode ? 'bg-slate-700' : 'bg-white'}`}
              >
                <value.icon className="w-12 h-12 text-blue-600 mb-4" />
                <h3 className={`text-xl font-semibold mb-2 ${isDarkMode ? 'text-white' : 'text-gray-900'}`}>
                  {value.title}
                </h3>
                <p className={`${isDarkMode ? 'text-gray-300' : 'text-gray-600'}`}>
                  {value.description}
                </p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Team Section */}
      <section className={`py-20 ${isDarkMode ? 'bg-slate-900' : 'bg-white'}`}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className={`text-3xl font-bold mb-4 ${isDarkMode ? 'text-white' : 'text-gray-900'}`}>
              Meet Our Team
            </h2>
            <p className={`text-lg ${isDarkMode ? 'text-gray-300' : 'text-gray-600'}`}>
              The people behind m-busi
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {teamMembers.map((member, index) => (
              <motion.div
                key={member.name}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: index * 0.1 }}
                className={`rounded-xl overflow-hidden shadow-sm ${isDarkMode ? 'bg-slate-700' : 'bg-white'}`}
              >
                <img
                  src={member.image}
                  alt={member.name}
                  className="w-full h-64 object-cover"
                />
                <div className="p-6">
                  <h3 className={`text-xl font-semibold mb-1 ${isDarkMode ? 'text-white' : 'text-gray-900'}`}>
                    {member.name}
                  </h3>
                  <p className={`mb-4 ${isDarkMode ? 'text-blue-400' : 'text-blue-600'}`}>
                    {member.role}
                  </p>
                  <p className={`${isDarkMode ? 'text-gray-300' : 'text-gray-600'}`}>
                    {member.bio}
                  </p>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Contact Section */}
      <section className={`py-20 ${isDarkMode ? 'bg-slate-800' : 'bg-gray-50'}`}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center">
            <h2 className={`text-3xl font-bold mb-4 ${isDarkMode ? 'text-white' : 'text-gray-900'}`}>
              Get in Touch
            </h2>
            <p className={`text-lg mb-8 ${isDarkMode ? 'text-gray-300' : 'text-gray-600'}`}>
              Have questions or suggestions? We'd love to hear from you.
            </p>
            <button className="px-8 py-3 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors">
              Contact Us
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}