import { Table, Badge } from 'react-bootstrap';
import type { MrBrowseItem } from '../types';

interface Props {
  items: MrBrowseItem[];
  reportId?: number;
}

function formatDate(iso: string | null): string {
  if (!iso) return '\u2014';
  return new Date(iso).toLocaleDateString();
}

function stateVariant(state: string): string {
  switch (state.toLowerCase()) {
    case 'merged':
      return 'success';
    case 'closed':
      return 'danger';
    case 'open':
    case 'opened':
      return 'primary';
    default:
      return 'secondary';
  }
}

export default function MrBrowseTable({ items, reportId }: Props) {
  function handleRowClick(item: MrBrowseItem) {
    if (reportId) {
      // If analysis exists, we don't have resultId mapping for browse items,
      // so open the original URL
      window.open(item.url, '_blank', 'noopener,noreferrer');
    } else {
      window.open(item.url, '_blank', 'noopener,noreferrer');
    }
  }

  return (
    <Table hover bordered responsive className="mr-browse-table">
      <thead className="table-dark">
        <tr>
          <th>#</th>
          <th>Tytul</th>
          <th>Autor</th>
          <th>Utworzono</th>
          <th>Status</th>
          <th>Pliki</th>
          <th>Etykiety</th>
        </tr>
      </thead>
      <tbody>
        {items.map((item) => (
          <tr
            key={item.externalId}
            className="clickable-row"
            onClick={() => handleRowClick(item)}
            style={{ cursor: 'pointer' }}
          >
            <td>{item.externalId}</td>
            <td>{item.title}</td>
            <td>{item.author}</td>
            <td>{formatDate(item.createdAt)}</td>
            <td>
              <Badge bg={stateVariant(item.state)}>{item.state}</Badge>
            </td>
            <td>{item.changedFilesCount}</td>
            <td>
              {item.labels.length > 0
                ? item.labels.map((l) => (
                    <Badge key={l} bg="info" text="dark" className="me-1">
                      {l}
                    </Badge>
                  ))
                : <span className="text-muted">-</span>}
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
