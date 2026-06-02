import React from 'react';

export default function PlaceholderPage({ name, icon }) {
  return (
    <div style={{ textAlign: 'center', paddingTop: 80 }}>
      {icon && <div style={{ fontSize: 48, marginBottom: 16 }}>{icon}</div>}
      <h2>{name}</h2>
      <p style={{ color: 'rgba(0,0,0,0.45)', marginTop: 16 }}>
        🚧 このページは現在開発中です
      </p>
    </div>
  );
}
