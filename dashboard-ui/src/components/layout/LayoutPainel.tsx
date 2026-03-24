import { Outlet } from 'react-router-dom';
import TopNav from './TopNav';

export default function LayoutPainel() {
  return (
    <div className="min-h-screen" style={{ backgroundColor: 'var(--color-bg)' }}>
      <TopNav />
      <main className="w-full px-4 py-4 overflow-auto">
        <Outlet />
      </main>
    </div>
  );
}
