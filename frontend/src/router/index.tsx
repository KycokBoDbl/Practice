import { createBrowserRouter, Navigate } from 'react-router-dom'

import { MainLayout } from '../layouts/MainLayout'
import { LoginPage } from '../pages/Login/LoginPage'
import { SpacesPage } from '../pages/Spaces/SpacesPage'
import { BookingPage } from '../pages/Booking/BookingPage'
import { ProfilePage } from '../pages/Profile/ProfilePage'
import { SpacePage } from '../pages/Spaces/SpacePage'

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
        element: <LoginPage />,
      },
      {
        path: 'spaces',
        children: [
          {
            index: true,
            element: <Navigate to="/" replace />,
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
        path: 'profile',
        element: <ProfilePage />,
      },
    ],
  },
])