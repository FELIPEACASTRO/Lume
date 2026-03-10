import { Routes, Route } from 'react-router-dom';
import AppShell from '../components/shell/AppShell';
import Home from '../pages/Home';
import Users from '../pages/Users';
import Agents from '../pages/Agents';
import Library from '../pages/Library';
import Projects from '../pages/Projects';
import SearchResults from '../pages/SearchResults';
import Tasks from '../pages/Tasks';
import TaskView from '../pages/TaskView';
import Usage from '../pages/Usage';
import Inbox from '../pages/Inbox';
import Settings from '../pages/Settings';
import NotFound from '../pages/NotFound';

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<AppShell />}>
        <Route index element={<Home />} />
        <Route path="users" element={<Users />} />
        <Route path="projects" element={<Projects />} />
        <Route path="search/results" element={<SearchResults />} />
        <Route path="tasks" element={<Tasks />} />
        <Route path="tasks/:taskId" element={<TaskView />} />
        <Route path="agents" element={<Agents />} />
        <Route path="library" element={<Library />} />
        <Route path="usage" element={<Usage />} />
        <Route path="inbox" element={<Inbox />} />
        <Route path="settings" element={<Settings />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}
