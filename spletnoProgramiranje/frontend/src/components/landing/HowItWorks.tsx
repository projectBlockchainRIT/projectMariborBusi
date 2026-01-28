import { UserPlus, Search, Navigation, Bus } from 'lucide-react';
import { useTheme } from '../../context/ThemeContext';

const steps = [
  {
    icon: UserPlus,
    number: "01",
    title: "Create Account",
    description: "Sign up in seconds with just your email. No credit card required.",
  },
  {
    icon: Search,
    number: "02",
    title: "Find Your Route",
    description: "Search for destinations or browse the interactive map.",
  },
  {
    icon: Navigation,
    number: "03",
    title: "Track in Real-Time",
    description: "See live bus locations and get accurate arrival predictions.",
  },
  {
    icon: Bus,
    number: "04",
    title: "Enjoy Your Ride",
    description: "Catch your bus on time and travel stress-free.",
  }
];

const HowItWorks = () => {
  const { isDarkMode } = useTheme();

  return (
    <section id="how-it-works" className={`py-24 ${isDarkMode ? 'bg-slate-800' : 'bg-slate-50'}`}>
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section header */}
        <div className="text-center max-w-2xl mx-auto mb-16">
          <span className="text-marprom-600 font-semibold text-sm uppercase tracking-wider">How It Works</span>
          <h2 className={`text-3xl sm:text-4xl font-bold mt-3 mb-4 tracking-tight ${
            isDarkMode ? 'text-white' : 'text-slate-900'
          }`}>
            Start in minutes
          </h2>
          <p className={`text-lg ${isDarkMode ? 'text-slate-400' : 'text-slate-600'}`}>
            Getting started with M-busi is quick and easy. Follow these simple steps.
          </p>
        </div>

        {/* Steps */}
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
          {steps.map((step, index) => {
            const Icon = step.icon;
            return (
              <div key={index} className="relative">
                {/* Connector line */}
                {index < steps.length - 1 && (
                  <div className={`hidden lg:block absolute top-12 left-full w-full h-0.5 -translate-x-1/2 z-0 ${
                    isDarkMode
                      ? 'bg-gradient-to-r from-marprom-600/30 to-transparent'
                      : 'bg-gradient-to-r from-marprom-200 to-transparent'
                  }`}></div>
                )}

                <div className={`relative rounded-2xl p-8 shadow-sm transition-all duration-300 ${
                  isDarkMode
                    ? 'bg-slate-900 border border-slate-700 hover:shadow-lg hover:shadow-slate-900/50'
                    : 'bg-white border border-slate-100 hover:shadow-lg'
                }`}>
                  {/* Step number */}
                  <span className="absolute -top-3 -right-3 bg-marprom-600 text-white text-sm font-bold w-9 h-9 rounded-full flex items-center justify-center shadow-lg shadow-marprom-600/30">
                    {step.number}
                  </span>

                  {/* Icon */}
                  <div className={`w-14 h-14 rounded-2xl flex items-center justify-center mb-6 ${
                    isDarkMode ? 'bg-marprom-600/10' : 'bg-marprom-50'
                  }`}>
                    <Icon className="h-7 w-7 text-marprom-600" />
                  </div>

                  {/* Content */}
                  <h3 className={`text-lg font-semibold mb-3 ${
                    isDarkMode ? 'text-white' : 'text-slate-900'
                  }`}>{step.title}</h3>
                  <p className={`text-sm leading-relaxed ${
                    isDarkMode ? 'text-slate-400' : 'text-slate-600'
                  }`}>{step.description}</p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
};

export default HowItWorks;
