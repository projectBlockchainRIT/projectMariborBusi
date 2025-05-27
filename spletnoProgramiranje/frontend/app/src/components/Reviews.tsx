import React from 'react';
import { Star, StarHalf } from 'lucide-react';

const reviews = [
  {
    name: "Maja Novak",
    avatar: "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2",
    rating: 5,
    review: "M-busi has completely transformed my daily commute in Maribor. The real-time tracking is incredibly accurate and the interface is so intuitive. I can't imagine going back to the old way of catching buses!",
    date: "March 15, 2025"
  },
  {
    name: "Luka Kovač",
    avatar: "https://images.pexels.com/photos/220453/pexels-photo-220453.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2",
    rating: 4.5,
    review: "As a student at University of Maribor, this app has saved me countless times from being late to classes. The route planning feature is exceptional, especially during peak hours when some routes get congested.",
    date: "February 28, 2025"
  },
  {
    name: "Ana Horvat",
    avatar: "https://images.pexels.com/photos/774909/pexels-photo-774909.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2",
    rating: 5,
    review: "The notifications for bus delays have been a game-changer for me. I use M-busi every day to commute to work, and it has never let me down. The digital ticket feature is also super convenient!",
    date: "January 10, 2025"
  }
];

const renderRating = (rating) => {
  const fullStars = Math.floor(rating);
  const hasHalfStar = rating % 1 !== 0;
  
  return (
    <div className="flex">
      {[...Array(fullStars)].map((_, i) => (
        <Star key={i} className="w-5 h-5 text-yellow-500 fill-yellow-500" />
      ))}
      {hasHalfStar && <StarHalf className="w-5 h-5 text-yellow-500 fill-yellow-500" />}
    </div>
  );
};

const Reviews = () => {
  return (
    <section id="reviews" className="py-16 md:py-24 bg-white">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-3xl mx-auto mb-16">
          <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">What Our Users Say</h2>
          <p className="text-lg text-gray-600">
            Join thousands of satisfied commuters who rely on M-busi for their daily travel needs in Maribor.
          </p>
        </div>
        
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {reviews.map((review, index) => (
            <div 
              key={index} 
              className="bg-gray-50 p-6 rounded-xl shadow-sm hover:shadow-md transition-shadow border border-gray-100"
            >
              <div className="flex items-center mb-4">
                <img 
                  src={review.avatar} 
                  alt={review.name} 
                  className="w-12 h-12 rounded-full object-cover mr-4"
                />
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">{review.name}</h3>
                  {renderRating(review.rating)}
                </div>
              </div>
              <p className="text-gray-600 mb-4 italic">"{review.review}"</p>
              <p className="text-sm text-gray-500">{review.date}</p>
            </div>
          ))}
        </div>
        
        <div className="mt-12 text-center">
          <div className="inline-flex items-center justify-center bg-mbusi-red-50 px-6 py-3 rounded-full">
            <div className="flex space-x-1">
              {[...Array(5)].map((_, i) => (
                <Star key={i} className="w-5 h-5 text-yellow-500 fill-yellow-500" />
              ))}
            </div>
            <span className="ml-3 font-semibold text-gray-900">4.8 out of 5 stars from over 1,200 reviews</span>
          </div>
        </div>
      </div>
    </section>
  );
};

export default Reviews;