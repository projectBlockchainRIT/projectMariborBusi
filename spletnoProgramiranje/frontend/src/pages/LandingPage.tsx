import { useTheme } from '../context/ThemeContext';
import Header from '../components/landing/Header';
import Hero from '../components/landing/Hero';
import Features from '../components/landing/Features';
import HowItWorks from '../components/landing/HowItWorks';
import Reviews from '../components/landing/Reviews';
import About from '../components/landing/About';
import CTA from '../components/landing/CTA';
import Footer from '../components/landing/Footer';

const LandingPage = () => {
  const { isDarkMode } = useTheme();

  return (
    <div className={`min-h-screen ${isDarkMode ? 'bg-slate-950' : 'bg-white'}`}>
      <Header />
      <main>
        <Hero />
        <Features />
        <HowItWorks />
        <Reviews />
        <About />
        <CTA />
      </main>
      <Footer />
    </div>
  );
};

export default LandingPage;
