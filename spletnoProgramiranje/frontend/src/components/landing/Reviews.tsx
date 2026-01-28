import { Star, Quote } from 'lucide-react';
import { useTheme } from '../../context/ThemeContext';

const reviews = [
  {
    name: "Maja Novak",
    role: "Daily Commuter",
    avatar: "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg?auto=compress&cs=tinysrgb&w=150",
    rating: 5,
    review: "M-busi has completely transformed my daily commute. The real-time tracking is incredibly accurate!",
  },
  {
    name: "Luka Kovac",
    role: "University Student",
    avatar: "https://images.pexels.com/photos/220453/pexels-photo-220453.jpeg?auto=compress&cs=tinysrgb&w=150",
    rating: 5,
    review: "As a student, this app has saved me countless times from being late to classes. Highly recommended!",
  },
  {
    name: "Ana Horvat",
    role: "Office Worker",
    avatar: "https://images.pexels.com/photos/774909/pexels-photo-774909.jpeg?auto=compress&cs=tinysrgb&w=150",
    rating: 5,
    review: "The delay notifications are a game-changer. I use M-busi every day and it never lets me down.",
  }
];

const Reviews = () => {
  const { isDarkMode } = useTheme();

  return (
    <section id="reviews" className={`py-24 ${isDarkMode ? 'bg-slate-900' : 'bg-white'}`}>
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section header */}
        <div className="text-center max-w-2xl mx-auto mb-16">
          <span className="text-marprom-600 font-semibold text-sm uppercase tracking-wider">Testimonials</span>
          <h2 className={`text-3xl sm:text-4xl font-bold mt-3 mb-4 tracking-tight ${
            isDarkMode ? 'text-white' : 'text-slate-900'
          }`}>
            Loved by commuters
          </h2>
          <p className={`text-lg ${isDarkMode ? 'text-slate-400' : 'text-slate-600'}`}>
            Join thousands of satisfied users who rely on M-busi every day.
          </p>
        </div>

        {/* Reviews grid */}
        <div className="grid md:grid-cols-3 gap-6">
          {reviews.map((review, index) => (
            <div
              key={index}
              className={`relative rounded-2xl p-8 transition-all duration-300 ${
                isDarkMode
                  ? 'bg-slate-800 border border-slate-700 hover:shadow-lg hover:shadow-slate-900/50'
                  : 'bg-slate-50 border border-slate-100 hover:shadow-lg'
              }`}
            >
              {/* Quote icon */}
              <Quote className={`absolute top-6 right-6 h-8 w-8 ${
                isDarkMode ? 'text-marprom-600/20' : 'text-marprom-100'
              }`} />

              {/* Stars */}
              <div className="flex gap-1 mb-4">
                {[...Array(review.rating)].map((_, i) => (
                  <Star key={i} className="w-5 h-5 text-amber-400 fill-amber-400" />
                ))}
              </div>

              {/* Review text */}
              <p className={`mb-6 leading-relaxed ${
                isDarkMode ? 'text-slate-300' : 'text-slate-700'
              }`}>"{review.review}"</p>

              {/* Author */}
              <div className="flex items-center gap-4">
                <img
                  src={review.avatar}
                  alt={review.name}
                  className={`w-11 h-11 rounded-full object-cover shadow-sm ${
                    isDarkMode ? 'ring-2 ring-slate-700' : 'ring-2 ring-white'
                  }`}
                />
                <div>
                  <p className={`font-semibold text-sm ${
                    isDarkMode ? 'text-white' : 'text-slate-900'
                  }`}>{review.name}</p>
                  <p className={`text-xs ${
                    isDarkMode ? 'text-slate-500' : 'text-slate-500'
                  }`}>{review.role}</p>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Rating summary */}
        <div className="mt-16 text-center">
          <div className={`inline-flex items-center gap-4 px-8 py-4 rounded-2xl ${
            isDarkMode
              ? 'bg-marprom-600/10 border border-marprom-600/20'
              : 'bg-marprom-50 border border-marprom-100'
          }`}>
            <div className="flex gap-1">
              {[...Array(5)].map((_, i) => (
                <Star key={i} className="w-5 h-5 text-amber-400 fill-amber-400" />
              ))}
            </div>
            <div className="text-left">
              <p className={`font-bold ${isDarkMode ? 'text-white' : 'text-slate-900'}`}>4.8 out of 5</p>
              <p className={`text-xs ${isDarkMode ? 'text-slate-400' : 'text-slate-600'}`}>from 1,200+ reviews</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default Reviews;
