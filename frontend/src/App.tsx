import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import DashboardPage from './pages/DashboardPage';
import MrDetailPage from './pages/MrDetailPage';
import AnalysisDetailPage from './pages/AnalysisDetailPage';
import { Alert } from 'react-bootstrap';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<DashboardPage />} />
          <Route path="mr/:reportId/:resultId" element={<MrDetailPage />} />
          <Route path="analysis/:reportId/:resultId" element={<AnalysisDetailPage />} />
          <Route path="*" element={<Alert variant="warning">Strona nie znaleziona (404)</Alert>} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
