import { Bus, Mail, Phone, MapPin, Facebook, Instagram, Twitter } from 'lucide-react';

const Footer = () => {
  return (
    <footer className="bg-gray-900 text-white pt-16 pb-8">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 mb-8">
          <div>
            <div className="flex items-center mb-4">
              <Bus className="h-8 w-8 text-mbusi-red-500" />
              <span className="ml-2 text-2xl font-bold text-white">M-busi</span>
            </div>
            <p className="text-gray-400 mb-6">
              The smart way to navigate Maribor's public transportation system with real-time tracking and intuitive route planning.
            </p>
            <div className="flex space-x-4">
              <a href="https://facebook.com/mbusi" className="text-gray-400 hover:text-mbusi-red-500 transition-colors" aria-label="Follow us on Facebook">
                 <Facebook className="h-5 w-5" />
               </a>
              <a href="https://instagram.com/mbusi" className="text-gray-400 hover:text-mbusi-red-500 transition-colors" aria-label="Follow us on Instagram">
                 <Instagram className="h-5 w-5" />
               </a>
              <a href="https://twitter.com/mbusi" className="text-gray-400 hover:text-mbusi-red-500 transition-colors" aria-label="Follow us on Twitter">
                 <Twitter className="h-5 w-5" />
               </a>
            </div>
          </div>
          
          <div>
            <h3 className="text-lg font-semibold mb-4 text-white">Quick Links</h3>
            <ul className="space-y-3">
              <li>
                <a href="#features" className="text-gray-400 hover:text-mbusi-red-500 transition-colors">Features</a>
              </li>
              <li>
                <a href="#how-it-works" className="text-gray-400 hover:text-mbusi-red-500 transition-colors">How It Works</a>
              </li>
              <li>
                <a href="#reviews" className="text-gray-400 hover:text-mbusi-red-500 transition-colors">Reviews</a>
              </li>
              <li>
                <a href="#about" className="text-gray-400 hover:text-mbusi-red-500 transition-colors">About</a>
              </li>
              <li>
                <a href="#faq" className="text-gray-400 hover:text-mbusi-red-500 transition-colors">FAQ</a>
              </li>
            </ul>
          </div>
          
          <div>
            <h3 className="text-lg font-semibold mb-4 text-white">Legal</h3>
            <ul className="space-y-3">
              <li>
                <a href="#" className="text-gray-400 hover:text-mbusi-red-500 transition-colors">Terms of Service</a>
              </li>
              <li>
                <a href="#" className="text-gray-400 hover:text-mbusi-red-500 transition-colors">Privacy Policy</a>
              </li>
              <li>
                <a href="#" className="text-gray-400 hover:text-mbusi-red-500 transition-colors">Cookie Policy</a>
              </li>
              <li>
                <a href="#" className="text-gray-400 hover:text-mbusi-red-500 transition-colors">Licensing</a>
              </li>
            </ul>
          </div>
          
          <div>
            <h3 className="text-lg font-semibold mb-4 text-white">Contact Marprom</h3>
            <ul className="space-y-4">
              <li className="flex items-start">
                <MapPin className="h-5 w-5 text-mbusi-red-500 mr-3 mt-1" />
                <span className="text-gray-400">Mlinska ulica 1, 2000 Maribor, Slovenia</span>
              </li>
              <li className="flex items-center">
                <Phone className="h-5 w-5 text-mbusi-red-500 mr-3" />
                <span className="text-gray-400">+386 2 300 10 00</span>
              </li>
              <li className="flex items-center">
                <Mail className="h-5 w-5 text-mbusi-red-500 mr-3" />
                <span className="text-gray-400">info@marprom.si</span>
              </li>
            </ul>
          </div>
        </div>
        
        <div className="pt-8 border-t border-gray-800 text-center">
          <p className="text-gray-500">
            &copy; {new Date().getFullYear()} M-busi by Marprom. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;