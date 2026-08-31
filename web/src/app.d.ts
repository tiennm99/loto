/// <reference types="vite-plugin-pwa/client" />

// See https://svelte.dev/docs/kit/types#app.d.ts for what these interfaces
// are for. None are used yet — this file's real job right now is the
// triple-slash reference above, which supplies the `virtual:pwa-register`
// module types that `+layout.svelte` imports.
declare global {
  namespace App {
    // interface Error {}
    // interface Locals {}
    // interface PageData {}
    // interface PageState {}
    // interface Platform {}
  }
}

export {};
