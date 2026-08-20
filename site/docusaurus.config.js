// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const {execFileSync} = require("node:child_process");
const {readFileSync} = require("node:fs");
const {resolve} = require("node:path");

const latestReleaseTag = execFileSync(
  "git",
  ["describe", "--tags", "--abbrev=0", "--match", "v[0-9]*"],
  {encoding: "utf8"},
).trim();

if (!/^v\d+\.\d+\.\d+$/.test(latestReleaseTag)) {
  throw new Error(`Invalid release tag: ${latestReleaseTag}`);
}

const latestRelease = latestReleaseTag.slice(1);

const kotlinVersion = readFileSync(
  resolve(__dirname, "../gradle/libs.versions.toml"),
  "utf8",
).match(/^kotlin\s*=\s*"([^"]+)"/m)?.[1];

if (!kotlinVersion) {
  throw new Error("Could not determine the Kotlin version from gradle/libs.versions.toml");
}

const config = /** @type {import("@docusaurus/types").Config} */ {
  title: "kotlin-tsgen",
  tagline: "Generate TypeScript interfaces from Kotlin classes",
  url: "https://esafak.github.io",
  baseUrl: "/kotlin-tsgen",
  onBrokenLinks: "throw",
  favicon: "img/icon.svg",

  markdown: {
    mdx1Compat: {
      comments: true,
    },
    hooks: {
      onBrokenMarkdownLinks: "warn",
    },
  },

  // GitHub pages deployment config
  organizationName: "esafak",
  projectName: "kotlin-tsgen",

  i18n: {defaultLocale: "en", locales: ["en"]},

  customFields: {
    latestRelease,
    kotlinVersion,
  },

  presets: [
    [
      "classic", /** @type {import("@docusaurus/preset-classic").Options} */ ({
      docs: {
        sidebarPath: require.resolve("./sidebars.js"),
        sidebarCollapsible: false,
        editUrl: "https://github.com/esafak/kotlin-tsgen/blob/main/",
      },
      theme: {
        customCss: [
          require.resolve("./src/css/custom.css"),
        ],
      },
    }),
    ],
  ],

  // scripts: [],

  clientModules: [
    require.resolve("./src/css/global.scss"),
  ],

  themeConfig: /** @type {import("@docusaurus/preset-classic").ThemeConfig} */ ({
    colorMode: {
      defaultMode: "dark",
      disableSwitch: false,
      respectPrefersColorScheme: true,
    },
    metadata: [{
      name: "keywords",
      content: "kotlin, typescript, json, transform, convert, generate"
    }],
    navbar: {
      title: "kotlin-tsgen",
      logo: {alt: "kotlin-tsgen logo", src: "img/icon.svg"},
      items: [
        // {to: "/getting-started", label: "Getting started", position: "left"},
        {type: "doc", docId: "getting-started", label: "Docs", position: "left"},
        {
          href: "https://github.com/esafak/kotlin-tsgen",
          label: "GitHub",
          position: "right",
        },
      ],
    },
    footer: {
      style: "dark",
      links: [
        {
          title: "Docs",
          items: [
            // {label: "Getting started", to: "/getting-started"},
            {label: "Docs", to: "/docs"},
            {label: "Examples", to: "/docs/examples"},
          ],
        },
        {
          title: "More",
          items: [
            {
              label: "GitHub",
              href: "https://github.com/esafak/kotlin-tsgen/",
            },
            {
              label: "Releases",
              href: "https://github.com/esafak/kotlin-tsgen/releases",
            },
            {
              label: "Help and Discussions",
              href: "https://github.com/esafak/kotlin-tsgen/discussions",
            },
            {
              label: "Issues and requests",
              href: "https://github.com/esafak/kotlin-tsgen/issues",
            },
          ]
        }
      ],
      copyright: `Copyright © 2026`,
    },
    prism: {
      // themes are managed by global.scss
      theme: {plain: {}, styles: []},
      darkTheme: {plain: {}, styles: []},
      additionalLanguages: ["kotlin", "typescript", "groovy", "markup"],
    },
  }),

  plugins: ["docusaurus-plugin-sass"],
};

module.exports = config;
