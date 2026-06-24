import '@testing-library/jest-dom';

// Polyfill ResizeObserver for jsdom
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};

// Polyfill matchMedia
global.matchMedia = global.matchMedia || function (query) {
  return {
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  };
};

// Polyfill getComputedStyle for pseudo-elements
const originalGetComputedStyle = global.getComputedStyle;
global.getComputedStyle = (elt, pseudoElt) => {
  const style = originalGetComputedStyle ? originalGetComputedStyle(elt, pseudoElt) : {};
  return {
    ...style,
    getPropertyValue: (prop) => style[prop] || '',
  };
};
