# API Configuration Fix

## Problem Identified

Your frontend had **two different backend IP addresses** hardcoded throughout the codebase:
- Login/Register used: `http://20.208.138.248:8080`
- Routes/Stations/Other APIs used: `http://40.68.198.73:8080`

This caused routes and stations APIs to fail while authentication worked.

## Solution Implemented

### 1. Created Centralized API Configuration

**File:** `src/config/api.ts`

This file now centralizes all API configuration with:
- `BACKEND_URL` - Gets from environment variable `VITE_BACKEND_URL` or defaults to localhost
- `getApiUrl(endpoint)` - Helper function to construct full API URLs
- `getAuthFetchOptions()` - Helper for authenticated requests

### 2. Created Environment Configuration

**Files:**
- `.env` - Your local configuration (gitignored)
- `.env.example` - Template for others

### 3. Updated All API Calls

Updated all files to use `getApiUrl()` instead of hardcoded URLs:
- ✅ Login.tsx
- ✅ Register.tsx
- ✅ DelaysController.tsx
- ✅ OccupancyController.tsx
- ✅ Hero.tsx
- ✅ InteractiveMap.tsx
- ✅ PassengerDensityGraph.tsx
- ✅ ActiveBusesProgress.tsx
- ✅ DelayAnalysis.tsx
- ✅ DelaysPage.tsx
- ✅ All utility files (api.ts, auth.ts, busStops.ts, drawRouteOnMap.ts, drawRoutesOnMap.ts)

## How to Use

### Testing Different Backend Servers

Edit `frontend/.env` to change the backend URL:

```bash
# Test with first Azure server
VITE_BACKEND_URL=http://20.208.138.248:8080

# Test with second Azure server
VITE_BACKEND_URL=http://40.68.198.73:8080

# Test with local backend
VITE_BACKEND_URL=http://localhost:8080
```

### After changing .env:

```bash
cd frontend
npm run dev  # Restart the dev server
```

Vite will automatically pick up the new environment variable.

## Testing the Fix

1. **Test Login** (should still work):
   ```
   Visit: http://localhost:5173/login
   Try logging in with your credentials
   ```

2. **Test Routes/Stations** (should now work):
   ```
   Visit: http://localhost:5173/dashboard/delays
   - Routes should load in the sidebar
   - Clicking a route should show stations
   ```

3. **Test Occupancy** (should now work):
   ```
   Visit: http://localhost:5173/dashboard/occupancy
   - Routes should load
   - Selecting a route should show occupancy data
   ```

## Determining the Correct Backend URL

Try each URL in your browser to see which one is actually running:

```bash
# Test routes endpoint
curl http://20.208.138.248:8080/v1/routes/list
curl http://40.68.198.73:8080/v1/routes/list

# Test auth endpoint
curl http://20.208.138.248:8080/v1/authentication/healthcheck
curl http://40.68.198.73:8080/v1/authentication/healthcheck
```

Use whichever one responds successfully.

## For Production Deployment

The `docker-compose.yml` already sets `VITE_BACKEND_URL=http://backend:8080` for containerized deployment.

## Troubleshooting

### If APIs still don't work:

1. **Check the dev console** in browser (F12):
   - Look for the actual URL being called
   - Check for CORS errors
   - Check for 404/500 errors

2. **Verify .env is loaded**:
   ```javascript
   // Add this temporarily to any component
   console.log('Backend URL:', import.meta.env.VITE_BACKEND_URL);
   ```

3. **Check backend is actually running**:
   ```bash
   # Test if backend responds
   curl http://20.208.138.248:8080/v1/routes/list
   ```

4. **Restart the dev server** after changing `.env`:
   ```bash
   # Stop the current dev server (Ctrl+C)
   npm run dev
   ```

### Common Issues:

- **CORS errors**: Backend needs to allow your frontend origin
- **404 errors**: Check if the backend endpoint exists
- **Network errors**: Check if the backend server is reachable
- **Empty responses**: Backend may be returning data in a different format

## Files Changed

### New Files:
- `frontend/src/config/api.ts` - Central API configuration
- `frontend/.env` - Local environment config
- `frontend/.env.example` - Environment config template
- `frontend/API_CONFIGURATION.md` - This file

### Modified Files:
- All components and utilities that make API calls (20+ files)
- Each file now imports and uses `getApiUrl()` from `src/config/api.ts`
