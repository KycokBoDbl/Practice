import { createBrowserRouter } from 'react-router-dom'

import { MainLayout } from '../layouts/MainLayout'
import { HomePage } from '../pages/Home/HomePage'
import { LoginPage } from '../pages/Login/LoginPage'
import { SpacesPage } from '../pages/Spaces/SpacesPage'
import { BookingPage } from '../pages/Booking/BookingPage'
import { ProfilePage } from '../pages/Profile/ProfilePage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <MainLayout />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: 'login',
        element: <LoginPage />,
      },
      {
        path: 'spaces',
        element: <SpacesPage />,
      },
      {
        path: 'booking',
        element: <BookingPage />,
      },
      {
        path: 'profile',
        element: <ProfilePage />,
      },
    ],
  },
])