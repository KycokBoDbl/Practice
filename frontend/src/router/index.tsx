import { createBrowserRouter, Navigate } from 'react-router-dom'

import { RequireAuth } from '../auth/RequireAuth'
import { RedirectAuthenticated } from '../auth/RedirectAuthenticated'
import { MainLayout } from '../layouts/MainLayout'
import { BookingPage } from '../pages/Booking/BookingPage'
import { BookingDetailPage } from '../pages/BookingDetail/BookingDetailPage'
import { BookingInboxPage } from '../pages/BookingInbox/BookingInboxPage'
import { ListingPublicationPage } from '../pages/ListingPublication/ListingPublicationPage'
import { LoginPage } from '../pages/Login/LoginPage'
import { MyListingsPage } from '../pages/MyListings/MyListingsPage'
import { ProfilePage } from '../pages/Profile/ProfilePage'
import { RegisterPage } from '../pages/Register/RegisterPage'
import { SpacePage } from '../pages/Spaces/SpacePage'
import { SpacesPage } from '../pages/Spaces/SpacesPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      {
        index: true,
        element: <SpacesPage />,
      },
      {
        path: 'login',
        element: (
          <RedirectAuthenticated>
            <LoginPage />
          </RedirectAuthenticated>
        ),
      },
      {
        path: 'register',
        element: (
          <RedirectAuthenticated>
            <RegisterPage />
          </RedirectAuthenticated>
        ),
      },
      {
        path: 'spaces',
        children: [
          {
            index: true,
            element: <Navigate to="/" replace />,
          },
          {
            path: 'new',
            element: (
              <RequireAuth>
                <ListingPublicationPage />
              </RequireAuth>
            ),
          },
          {
            path: ':id',
            element: <SpacePage />,
          },
        ],
      },
      {
        path: 'my-listings',
        element: (
          <RequireAuth>
            <MyListingsPage />
          </RequireAuth>
        ),
      },
      {
        path: 'booking/:id',
        element: <BookingPage />,
      },
      {
        path: 'bookings',
        element: (
          <RequireAuth>
            <BookingInboxPage />
          </RequireAuth>
        ),
      },
      {
        path: 'bookings/:bookingId',
        element: (
          <RequireAuth>
            <BookingDetailPage />
          </RequireAuth>
        ),
      },
      {
        path: 'profile',
        element: (
          <RequireAuth>
            <ProfilePage />
          </RequireAuth>
        ),
      },
    ],
  },
])
