import { Bus, Mail, Phone, MapPin } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useTheme } from '../../context/ThemeContext';

const Footer = () => {
  const currentYear = new Date().getFullYear();
  const { isDarkMode } = useTheme();

  return (
    <footer className={isDarkMode ? 'bg-slate-950 text-white' : 'bg-slate-900 text-white'}>
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        {/* Main footer content */}
        <div className="py-16 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12">
          {/* Brand */}
          <div className="lg:col-span-1">
            <Link to="/" className="flex items-center gap-2.5 mb-4">
              <div className="bg-marprom-600 p-2 rounded-xl">
                <Bus className="h-5 w-5 text-white" />
              </div>
              <span className="text-xl font-bold">M-busi</span>
            </Link>
            <p className="text-slate-400 text-sm leading-relaxed">
              The smart way to navigate Maribor's public transportation system.
            </p>
          </div>

          {/* Quick Links */}
          <div>
            <h3 className="font-semibold mb-4 text-sm">Quick Links</h3>
            <ul className="space-y-3">
              {['Features', 'How It Works', 'Reviews', 'About'].map((item) => (
                <li key={item}>
                  <a
                    href={`#${item.toLowerCase().replace(' ', '-')}`}
                    className="text-slate-400 hover:text-marprom-500 transition-colors text-sm"
                  >
                    {item}
                  </a>
                </li>
              ))}
            </ul>
          </div>

          {/* Legal */}
          <div>
            <h3 className="font-semibold mb-4 text-sm">Legal</h3>
            <ul className="space-y-3">
              {['Terms of Service', 'Privacy Policy', 'Cookie Policy'].map((item) => (
                <li key={item}>
                  <a href="#" className="text-slate-400 hover:text-marprom-500 transition-colors text-sm">
                    {item}
                  </a>
                </li>
              ))}
            </ul>
          </div>

          {/* Contact */}
          <div>
            <h3 className="font-semibold mb-4 text-sm">Contact Marprom</h3>
            <ul className="space-y-3">
              <li className="flex items-start gap-3">
                <MapPin className="h-4 w-4 text-marprom-500 mt-0.5 flex-shrink-0" />
                <span className="text-slate-400 text-sm">Mlinska ulica 1, 2000 Maribor</span>
              </li>
              <li className="flex items-center gap-3">
                <Phone className="h-4 w-4 text-marprom-500 flex-shrink-0" />
                <span className="text-slate-400 text-sm">+386 2 300 10 00</span>
              </li>
              <li className="flex items-center gap-3">
                <Mail className="h-4 w-4 text-marprom-500 flex-shrink-0" />
                <span className="text-slate-400 text-sm">info@marprom.si</span>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom bar */}
        <div className={`py-6 text-center ${
          isDarkMode ? 'border-t border-slate-800' : 'border-t border-slate-800'
        }`}>
          <p className="text-slate-500 text-sm">
            {currentYear} M-busi by Marprom. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
