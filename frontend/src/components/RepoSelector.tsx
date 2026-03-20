import { useState } from 'react';
import { Form, Button, Row, Col, ListGroup, Badge } from 'react-bootstrap';
import type { SavedRepository } from '../types';

interface Props {
  savedRepos: SavedRepository[];
  onSelect: (slug: string, provider: string) => void;
  onDelete: (id: number) => void;
  selectedSlug: string;
  selectedProvider: string;
}

export default function RepoSelector({
  savedRepos,
  onSelect,
  onDelete,
  selectedSlug,
  selectedProvider,
}: Props) {
  const [newSlug, setNewSlug] = useState('');
  const [newProvider, setNewProvider] = useState('github');

  function handleSelectFromList(repo: SavedRepository) {
    onSelect(repo.projectSlug, repo.provider);
  }

  function handleDeleteClick(e: React.MouseEvent, id: number) {
    e.stopPropagation();
    if (window.confirm('Usunac repozytorium z listy?')) {
      onDelete(id);
    }
  }

  function handleNewSlugSubmit() {
    const slug = newSlug.trim();
    if (slug) {
      onSelect(slug, newProvider);
      setNewSlug('');
    }
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleNewSlugSubmit();
    }
  }

  return (
    <div className="repo-selector">
      {savedRepos.length > 0 && (
        <div className="mb-3">
          <Form.Label className="fw-semibold">Zapisane repozytoria</Form.Label>
          <ListGroup>
            {savedRepos.map((repo) => (
              <ListGroup.Item
                key={repo.id}
                action
                active={selectedSlug === repo.projectSlug && selectedProvider === repo.provider}
                onClick={() => handleSelectFromList(repo)}
                className="d-flex justify-content-between align-items-center"
              >
                <div>
                  <span className="fw-medium">{repo.projectSlug}</span>
                  <Badge bg="secondary" className="ms-2">{repo.provider}</Badge>
                  {repo.lastAnalyzedAt && (
                    <small className="text-muted ms-2">
                      ostatnia analiza: {new Date(repo.lastAnalyzedAt).toLocaleDateString()}
                    </small>
                  )}
                </div>
                <Button
                  variant="outline-danger"
                  size="sm"
                  onClick={(e) => handleDeleteClick(e, repo.id)}
                  title="Usun"
                >
                  &times;
                </Button>
              </ListGroup.Item>
            ))}
          </ListGroup>
        </div>
      )}

      <Form.Label className="fw-semibold">Nowe repozytorium</Form.Label>
      <Row className="g-2 align-items-end">
        <Col md={6}>
          <Form.Control
            type="text"
            placeholder="owner/repo"
            value={newSlug}
            onChange={(e) => setNewSlug(e.target.value)}
            onBlur={handleNewSlugSubmit}
            onKeyDown={handleKeyDown}
          />
          <Form.Text className="text-muted">np. octocat/Hello-World</Form.Text>
        </Col>
        <Col md={3}>
          <Form.Select
            value={newProvider}
            onChange={(e) => setNewProvider(e.target.value)}
          >
            <option value="github">GitHub</option>
            <option value="gitlab">GitLab</option>
          </Form.Select>
        </Col>
        <Col md={3}>
          <Button variant="outline-primary" onClick={handleNewSlugSubmit}>
            Wybierz
          </Button>
        </Col>
      </Row>
    </div>
  );
}
