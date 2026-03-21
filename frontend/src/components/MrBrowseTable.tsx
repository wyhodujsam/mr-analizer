import { Table, Badge, Form } from 'react-bootstrap';
import type { MrBrowseItem } from '../types';
import { formatDate } from '../utils/format';

interface Props {
  items: MrBrowseItem[];
  selectedIds: Set<string>;
  onSelectionChange: (selectedIds: Set<string>) => void;
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

export default function MrBrowseTable({ items, selectedIds, onSelectionChange }: Props) {
  const allSelected = items.length > 0 && items.every(i => selectedIds.has(i.externalId));

  function toggleAll() {
    if (allSelected) {
      onSelectionChange(new Set());
    } else {
      onSelectionChange(new Set(items.map(i => i.externalId)));
    }
  }

  function toggleOne(id: string) {
    const next = new Set(selectedIds);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    onSelectionChange(next);
  }

  function handleRowClick(e: React.MouseEvent, item: MrBrowseItem) {
    // Don't open link if clicking checkbox
    if ((e.target as HTMLElement).closest('input[type="checkbox"]')) return;
    window.open(item.url, '_blank', 'noopener,noreferrer');
  }

  return (
    <Table hover bordered responsive className="mr-browse-table">
      <thead className="table-dark">
        <tr>
          <th style={{ width: 40 }}>
            <Form.Check
              type="checkbox"
              checked={allSelected}
              onChange={toggleAll}
              aria-label="Zaznacz wszystkie"
            />
          </th>
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
            className={`clickable-row ${selectedIds.has(item.externalId) ? 'table-active' : ''}`}
            onClick={(e) => handleRowClick(e, item)}
            style={{ cursor: 'pointer' }}
          >
            <td onClick={(e) => e.stopPropagation()}>
              <Form.Check
                type="checkbox"
                checked={selectedIds.has(item.externalId)}
                onChange={() => toggleOne(item.externalId)}
                aria-label={`Zaznacz MR #${item.externalId}`}
              />
            </td>
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
