import { useState } from 'react';
import { Form, Button, Spinner, Row, Col } from 'react-bootstrap';
import type { AnalysisRequest } from '../types';

interface Props {
  onSubmit: (request: AnalysisRequest) => void;
  loading: boolean;
}

export default function AnalysisForm({ onSubmit, loading }: Props) {
  const [projectSlug, setProjectSlug] = useState('');
  const [provider, setProvider] = useState('github');
  const [targetBranch, setTargetBranch] = useState('');
  const [after, setAfter] = useState('');
  const [before, setBefore] = useState('');
  const [limit, setLimit] = useState(100);
  const [useLlm, setUseLlm] = useState(false);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const req: AnalysisRequest = {
      projectSlug,
      provider,
      limit,
      useLlm,
    };
    if (targetBranch) req.targetBranch = targetBranch;
    if (after) req.after = after;
    if (before) req.before = before;
    onSubmit(req);
  }

  return (
    <Form onSubmit={handleSubmit}>
      <Row className="mb-3">
        <Col md={6}>
          <Form.Group controlId="projectSlug">
            <Form.Label>Project Slug <span className="text-danger">*</span></Form.Label>
            <Form.Control
              type="text"
              placeholder="owner/repo"
              value={projectSlug}
              onChange={(e) => setProjectSlug(e.target.value)}
              required
            />
            <Form.Text className="text-muted">e.g. octocat/Hello-World</Form.Text>
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group controlId="provider">
            <Form.Label>Provider</Form.Label>
            <Form.Select
              value={provider}
              onChange={(e) => setProvider(e.target.value)}
            >
              <option value="github">GitHub</option>
            </Form.Select>
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group controlId="targetBranch">
            <Form.Label>Target Branch</Form.Label>
            <Form.Control
              type="text"
              placeholder="main"
              value={targetBranch}
              onChange={(e) => setTargetBranch(e.target.value)}
            />
          </Form.Group>
        </Col>
      </Row>

      <Row className="mb-3">
        <Col md={3}>
          <Form.Group controlId="after">
            <Form.Label>After</Form.Label>
            <Form.Control
              type="date"
              value={after}
              onChange={(e) => setAfter(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group controlId="before">
            <Form.Label>Before</Form.Label>
            <Form.Control
              type="date"
              value={before}
              onChange={(e) => setBefore(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col md={3}>
          <Form.Group controlId="limit">
            <Form.Label>Limit</Form.Label>
            <Form.Control
              type="number"
              min={1}
              max={500}
              value={limit}
              onChange={(e) => setLimit(Number(e.target.value))}
            />
          </Form.Group>
        </Col>
        <Col md={3} className="d-flex align-items-end pb-1">
          <Form.Check
            type="checkbox"
            id="useLlm"
            label="Use LLM analysis"
            checked={useLlm}
            onChange={(e) => setUseLlm(e.target.checked)}
          />
        </Col>
      </Row>

      <Button type="submit" variant="primary" disabled={loading}>
        {loading ? (
          <>
            <Spinner animation="border" size="sm" className="me-2" />
            Analyzing...
          </>
        ) : (
          'Run Analysis'
        )}
      </Button>
    </Form>
  );
}
