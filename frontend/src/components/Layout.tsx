import { Navbar, Nav, Container } from 'react-bootstrap';
import { Outlet, Link } from 'react-router-dom';

export default function Layout() {
  return (
    <>
      <Navbar bg="dark" variant="dark" expand="lg">
        <Container>
          <Navbar.Brand as={Link} to="/">MR Analizer</Navbar.Brand>
          <Nav className="ms-auto">
            <Nav.Link as={Link} to="/">Analiza PR</Nav.Link>
            <Nav.Link as={Link} to="/activity">Aktywność</Nav.Link>
          </Nav>
        </Container>
      </Navbar>
      <Container className="mt-4">
        <Outlet />
      </Container>
    </>
  );
}
