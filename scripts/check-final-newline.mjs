import fs from "node:fs";

const listPath = process.argv[2];
if (!listPath) {
  console.error("usage: check-final-newline.mjs <nul-delimited-path-list>");
  process.exit(2);
}

const names = fs.readFileSync(listPath).toString("utf8").split("\0").filter(Boolean);
for (const name of names) {
  if (!fs.statSync(name).isFile()) continue;

  const fd = fs.openSync(name, "r");
  const buffer = Buffer.alloc(64 * 1024);
  let size = 0;
  let last = -1;
  let binary = false;
  try {
    while (true) {
      const count = fs.readSync(fd, buffer, 0, buffer.length, null);
      if (count === 0) break;
      size += count;
      last = buffer[count - 1];
      if (!binary && buffer.subarray(0, count).includes(0)) binary = true;
    }
  } finally {
    fs.closeSync(fd);
  }

  if (!binary && size > 0 && last !== 0x0a && last !== 0x0d) {
    console.error(`${name}: missing final newline`);
    process.exitCode = 1;
  }
}
