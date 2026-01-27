import { Star, Quote } from 'lucide-react';

const reviews = [
  {
    name: "Maja Novak",
    role: "Daily Commuter",
    avatar: "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg?auto=compress&cs=tinysrgb&w=150",
    rating: 5,
    review: "M-busi has completely transformed my daily commute. The real-time tracking is incredibly accurate!",
  },
  {
    name: "Luka Kovač",
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
  return (
    <section id="reviews" className="py-24 bg-white">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        {/* Section header */}
        <div className="text-center max-w-2xl mx-auto mb-16">
          <span className="text-marprom-600 font-semibold text-sm uppercase tracking-wider">Testimonials</span>
          <h2 className="text-4xl font-bold text-gray-900 mt-3 mb-4">
            Loved by commuters
          </h2>
          <p className="text-lg text-gray-600">
            Join thousands of satisfied users who rely on M-busi every day.
          </p>
        </div>

        {/* Reviews grid */}
        <div className="grid md:grid-cols-3 gap-8">
          {reviews.map((review, index) => (
            <div
              key={index}
              className="relative bg-gray-50 rounded-2xl p-8 hover:shadow-lg transition-shadow duration-300"
            >
              {/* Quote icon */}
              <Quote className="absolute top-6 right-6 h-8 w-8 text-marprom-100" />

              {/* Stars */}
              <div className="flex gap-1 mb-4">
                {[...Array(review.rating)].map((_, i) => (
                  <Star key={i} className="w-5 h-5 text-yellow-400 fill-yellow-400" />
                ))}
              </div>

              {/* Review text */}
              <p className="text-gray-700 mb-6 leading-relaxed">"{review.review}"</p>

              {/* Author */}
              <div className="flex items-center gap-4">
                <img
                  src={review.avatar}
                  alt={review.name}
                  className="w-12 h-12 rounded-full object-cover"
                />
                <div>
                  <p className="font-semibold text-gray-900">{review.name}</p>
                  <p className="text-sm text-gray-500">{review.role}</p>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Rating summary */}
        <div className="mt-16 text-center">
          <div className="inline-flex items-center gap-4 bg-marprom-50 px-8 py-4 rounded-2xl">
            <div className="flex gap-1">
              {[...Array(5)].map((_, i) => (
                <Star key={i} className="w-6 h-6 text-yellow-400 fill-yellow-400" />
              ))}
            </div>
            <div className="text-left">
              <p className="font-bold text-gray-900">4.8 out of 5</p>
              <p className="text-sm text-gray-600">from 1,200+ reviews</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

export default Reviews;
