import { createBrowserRouter, Navigate } from 'react-router-dom'

import { MainLayout } from '../layouts/MainLayout'
import { RedirectAuthenticated } from '../auth/RedirectAuthenticated'
import { RequireAuth } from '../auth/RequireAuth'
import { LoginPage } from '../pages/Login/LoginPage'
import { RegisterPage } from '../pages/Register/RegisterPage'
import { SpacesPage } from '../pages/Spaces/SpacesPage'
import { BookingPage } from '../pages/Booking/BookingPage'
import { BookingDetailPage } from '../pages/BookingDetail/BookingDetailPage'
import { BookingInboxPage } from '../pages/BookingInbox/BookingInboxPage'
import { ProfilePage } from '../pages/Profile/ProfilePage'
import { SpacePage } from '../pages/Spaces/SpacePage'
import { ListingPublicationPage } from '../pages/ListingPublication/ListingPublicationPage'

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
