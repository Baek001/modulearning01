import { existsSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import process from 'node:process';
import { build } from 'vite';
import react from '@vitejs/plugin-react';

const ASCII_ALIAS_ENV = 'STARWORKS_BUILD_ASCII_ALIAS';

function containsNonAscii(value) {
  return /[^\x00-\x7F]/.test(value);
}

function availableDriveLetter() {
  for (const letter of ['Z', 'Y', 'X', 'W', 'V', 'U', 'T']) {
    if (!existsSync(`${letter}:\\`)) {
      return letter;
    }
  }

  throw new Error('No available drive letter for temporary build alias.');
}

function runCommand(file, args) {
  const result = spawnSync(file, args, {
    stdio: 'inherit',
    windowsHide: true,
  });

  if (result.status !== 0) {
    throw new Error(`Command failed: ${file} ${args.join(' ')}`);
  }
}

async function runBuildFromAsciiAlias() {
  const projectRoot = process.cwd();
  const driveLetter = availableDriveLetter();
  const aliasRoot = `${driveLetter}:\\`;
  const aliasScript = path.win32.join(aliasRoot, 'scripts', 'build.mjs');

  runCommand('subst.exe', [`${driveLetter}:`, projectRoot]);

  try {
    const result = spawnSync(process.execPath, [aliasScript], {
      cwd: aliasRoot,
      env: {
        ...process.env,
        [ASCII_ALIAS_ENV]: '1',
      },
      stdio: 'inherit',
      windowsHide: true,
    });

    process.exit(result.status ?? 1);
  } finally {
    spawnSync('subst.exe', [`${driveLetter}:`, '/d'], {
      stdio: 'inherit',
      windowsHide: true,
    });
  }
}

if (process.platform === 'win32' && containsNonAscii(process.cwd()) && process.env[ASCII_ALIAS_ENV] !== '1') {
  await runBuildFromAsciiAlias();
}

const projectRoot = process.cwd().replace(/\\/g, '/');

await build({
  root: projectRoot,
  resolve: {
    preserveSymlinks: true,
  },
  plugins: [react()],
});
