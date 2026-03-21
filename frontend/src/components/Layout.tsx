import { Navbar, Container } from 'react-bootstrap';
import { Outlet, Link } from 'react-router-dom';

export default function Layout() {
  return (
    <>
      <Navbar bg="dark" variant="dark" expand="lg">
        <Container>
          <Navbar.Brand as={Link} to="/">MR Analizer</Navbar.Brand>
        </Container>
      </Navbar>
      <Container className="mt-4">
        <Outlet />
      </Container>
    </>
  );
}
