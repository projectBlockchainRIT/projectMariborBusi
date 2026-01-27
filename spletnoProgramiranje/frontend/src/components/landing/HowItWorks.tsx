import { UserPlus, Search, Navigation, Bus } from 'lucide-react';

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
  return (
    <section id="how-it-works" className="py-24 bg-gray-50">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section header */}
        <div className="text-center max-w-2xl mx-auto mb-16">
          <span className="text-marprom-600 font-semibold text-sm uppercase tracking-wider">How It Works</span>
          <h2 className="text-4xl font-bold text-gray-900 mt-3 mb-4">
            Start in minutes
          </h2>
          <p className="text-lg text-gray-600">
            Getting started with M-busi is quick and easy. Follow these simple steps.
          </p>
        </div>

        {/* Steps */}
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
          {steps.map((step, index) => {
            const Icon = step.icon;
            return (
              <div key={index} className="relative">
                {/* Connector line */}
                {index < steps.length - 1 && (
                  <div className="hidden lg:block absolute top-12 left-full w-full h-0.5 bg-gradient-to-r from-marprom-200 to-transparent -translate-x-1/2 z-0"></div>
                )}

                <div className="relative bg-white rounded-2xl p-8 shadow-sm hover:shadow-lg transition-shadow duration-300">
                  {/* Step number */}
                  <span className="absolute -top-3 -right-3 bg-marprom-600 text-white text-sm font-bold w-10 h-10 rounded-full flex items-center justify-center shadow-lg">
                    {step.number}
                  </span>

                  {/* Icon */}
                  <div className="w-16 h-16 bg-marprom-50 rounded-2xl flex items-center justify-center mb-6">
                    <Icon className="h-8 w-8 text-marprom-600" />
                  </div>

                  {/* Content */}
                  <h3 className="text-xl font-semibold text-gray-900 mb-3">{step.title}</h3>
                  <p className="text-gray-600">{step.description}</p>
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
