import js from "@eslint/js";
import svelte from "eslint-plugin-svelte";
import globals from "globals";

export default [
  js.configs.recommended,
  ...svelte.configs["flat/recommended"],
  {
    languageOptions: {
      ecmaVersion: "latest",
      sourceType: "module",
      globals: {
        ...globals.browser,
        ...globals.node,
        // Svelte 5 runes — usable inside .svelte and .svelte.js files.
        $state: "readonly",
        $derived: "readonly",
        $effect: "readonly",
        $props: "readonly",
        $bindable: "readonly",
        $inspect: "readonly",
        $host: "readonly",
      },
    },
  },
  {
    ignores: [
      "build/**",
      ".svelte-kit/**",
      "node_modules/**",
      "plans/**",
      "docs/**",
    ],
  },
];
