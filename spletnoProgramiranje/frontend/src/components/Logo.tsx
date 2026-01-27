import { motion } from 'framer-motion';
import { TruckIcon } from '@heroicons/react/24/outline';

export default function Logo() {
  return (
    <motion.div
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      className="flex flex-col items-center"
    >
      <motion.div
        whileHover={{ scale: 1.1 }}
        whileTap={{ scale: 0.95 }}
        className="w-16 h-16 bg-indigo-600 rounded-full flex items-center justify-center mb-2"
      >
        <TruckIcon className="w-8 h-8 text-white" />
      </motion.div>
      <motion.h1
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.2 }}
        className="text-2xl font-bold text-gray-900 dark:text-white"
      >
        M-BUSI
      </motion.h1>
    </motion.div>
  );
} 