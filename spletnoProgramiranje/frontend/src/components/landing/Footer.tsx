import { Bus, Mail, Phone, MapPin } from 'lucide-react';
import { Link } from 'react-router-dom';

const Footer = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="bg-gray-900 text-white">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        {/* Main footer content */}
        <div className="py-16 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12">
          {/* Brand */}
          <div className="lg:col-span-1">
            <Link to="/" className="flex items-center gap-2 mb-4">
              <div className="bg-marprom-600 p-2 rounded-xl">
                <Bus className="h-6 w-6 text-white" />
              </div>
              <span className="text-2xl font-bold">M-busi</span>
            </Link>
            <p className="text-gray-400 mb-6">
              The smart way to navigate Maribor's public transportation system.
            </p>
          </div>

          {/* Quick Links */}
          <div>
            <h3 className="font-semibold mb-4">Quick Links</h3>
            <ul className="space-y-3">
              {['Features', 'How It Works', 'Reviews', 'About'].map((item) => (
                <li key={item}>
                  <a
                    href={`#${item.toLowerCase().replace(' ', '-')}`}
                    className="text-gray-400 hover:text-marprom-500 transition-colors"
                  >
                    {item}
                  </a>
                </li>
              ))}
            </ul>
          </div>

          {/* Legal */}
          <div>
            <h3 className="font-semibold mb-4">Legal</h3>
            <ul className="space-y-3">
              {['Terms of Service', 'Privacy Policy', 'Cookie Policy'].map((item) => (
                <li key={item}>
                  <a href="#" className="text-gray-400 hover:text-marprom-500 transition-colors">
                    {item}
                  </a>
                </li>
              ))}
            </ul>
          </div>

          {/* Contact */}
          <div>
            <h3 className="font-semibold mb-4">Contact Marprom</h3>
            <ul className="space-y-3">
              <li className="flex items-start gap-3">
                <MapPin className="h-5 w-5 text-marprom-500 mt-0.5" />
                <span className="text-gray-400">Mlinska ulica 1, 2000 Maribor</span>
              </li>
              <li className="flex items-center gap-3">
                <Phone className="h-5 w-5 text-marprom-500" />
                <span className="text-gray-400">+386 2 300 10 00</span>
              </li>
              <li className="flex items-center gap-3">
                <Mail className="h-5 w-5 text-marprom-500" />
                <span className="text-gray-400">info@marprom.si</span>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom bar */}
        <div className="py-6 border-t border-gray-800 text-center">
          <p className="text-gray-500 text-sm">
            &copy; {currentYear} M-busi by Marprom. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;
