"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  generateGrid,
  loadCrossedState,
  loadGrid,
  saveCrossedState,
  saveGrid,
} from "./loto-game-logic";

/** Check if a row has all its numbers crossed */
function isRowComplete(
  grid: number[][],
  crossed: boolean[][],
  row: number
): boolean {
  for (let col = 0; col < 9; col++) {
    if (grid[row][col] > 0 && !crossed[row]?.[col]) return false;
  }
  return true;
}

/** Find the single remaining uncrossed number in a row, or null if != 1 remaining */
function getWaitingNumber(
  grid: number[][],
  crossed: boolean[][],
  row: number
): number | null {
  let remaining: number | null = null;
  for (let col = 0; col < 9; col++) {
    if (grid[row][col] > 0 && !crossed[row]?.[col]) {
      if (remaining !== null) return null; // more than 1 remaining
      remaining = grid[row][col];
    }
  }
  return remaining;
}

export default function Home() {
  const [grid, setGrid] = useState<number[][] | null>(null);
  const [crossed, setCrossed] = useState<boolean[][]>([]);
  const [showInstructions, setShowInstructions] = useState(false);
  const [showCongrats, setShowCongrats] = useState(false);
  const [congratsRow, setCongratsRow] = useState<number>(-1);
  const [toast, setToast] = useState<string | null>(null);
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const celebratedRows = useRef<Set<number>>(new Set());
  const notifiedWaitingRows = useRef<Set<number>>(new Set());

  const dismissToast = useCallback(() => {
    setToast(null);
    if (toastTimer.current) {
      clearTimeout(toastTimer.current);
      toastTimer.current = null;
    }
  }, []);

  const showToast = useCallback(
    (msg: string) => {
      dismissToast();
      setToast(msg);
      toastTimer.current = setTimeout(() => setToast(null), 5000);
    },
    [dismissToast]
  );

  useEffect(() => {
    const savedGrid = loadGrid();
    if (savedGrid) {
      setGrid(savedGrid);
      const savedCrossed =
        loadCrossedState() ?? savedGrid.map((row) => row.map(() => false));
      setCrossed(savedCrossed);
      // Mark already-completed and already-waiting rows
      for (let i = 0; i < savedGrid.length; i++) {
        if (isRowComplete(savedGrid, savedCrossed, i)) {
          celebratedRows.current.add(i);
        }
        if (getWaitingNumber(savedGrid, savedCrossed, i) !== null) {
          notifiedWaitingRows.current.add(i);
        }
      }
    }
  }, []);

  useEffect(() => {
    if (crossed.length > 0) saveCrossedState(crossed);
  }, [crossed]);

  // Detect newly completed rows and waiting rows
  useEffect(() => {
    if (!grid || crossed.length === 0) return;

    for (let i = 0; i < grid.length; i++) {
      // Check for bingo first
      if (!celebratedRows.current.has(i) && isRowComplete(grid, crossed, i)) {
        celebratedRows.current.add(i);
        notifiedWaitingRows.current.add(i);
        setCongratsRow(i + 1);
        setShowCongrats(true);
        return;
      }
      // Check for 4/5 (waiting)
      const waitNum = getWaitingNumber(grid, crossed, i);
      if (waitNum !== null && !notifiedWaitingRows.current.has(i)) {
        notifiedWaitingRows.current.add(i);
        showToast(`Chờ ${waitNum}`);
        return;
      }
      // Reset waiting notification if row no longer has exactly 1 remaining
      if (waitNum === null && notifiedWaitingRows.current.has(i) && !celebratedRows.current.has(i)) {
        notifiedWaitingRows.current.delete(i);
      }
    }
  }, [grid, crossed, showToast]);

  const handleGenerate = useCallback(() => {
    if (grid && !confirm("Bạn có muốn tạo lại bảng không?")) return;
    const newGrid = generateGrid();
    const newCrossed = newGrid.map((row) => row.map(() => false));
    setGrid(newGrid);
    setCrossed(newCrossed);
    saveGrid(newGrid);
    saveCrossedState(newCrossed);
    celebratedRows.current.clear();
    notifiedWaitingRows.current.clear();
    dismissToast();
  }, [grid, dismissToast]);

  const handleCellClick = useCallback((row: number, col: number) => {
    setCrossed((prev) => {
      const next = prev.map((r) => [...r]);
      next[row][col] = !next[row][col];
      return next;
    });
  }, []);

  return (
    <div className="flex flex-col flex-1 items-center px-3 py-8 sm:py-12">
      <div className="w-full max-w-lg">
        {/* Header */}
        <header className="text-center mb-8">
          <h1 className="text-4xl sm:text-5xl font-extrabold tracking-tight bg-gradient-to-r from-indigo-500 to-purple-500 bg-clip-text text-transparent">
            Lô tô
          </h1>
          <p className="mt-2 text-sm text-slate-500 dark:text-slate-400">
            Lấy cảm hứng từ những buổi họp lớp thiếu giấy chơi lô tô
            <br className="hidden sm:block" /> của TN1 (2014–2017)
          </p>
          <div className="mt-3 flex items-center justify-center gap-3 text-xs">
            <button
              onClick={() => setShowInstructions((v) => !v)}
              className="text-indigo-500 dark:text-indigo-400 hover:underline"
            >
              {showInstructions ? "Ẩn hướng dẫn" : "Hướng dẫn"}
            </button>
            <span className="text-slate-300 dark:text-slate-600">|</span>
            <Link
              href="/master"
              className="text-orange-500 dark:text-orange-400 hover:underline"
            >
              Trang quản trò →
            </Link>
          </div>
        </header>

        {/* Instructions */}
        {showInstructions && (
          <div className="mb-6 rounded-xl bg-indigo-50 dark:bg-indigo-950/30 border border-indigo-100 dark:border-indigo-900 p-4 text-sm text-slate-600 dark:text-slate-400">
            <ul className="space-y-1 list-disc list-inside">
              <li>
                Nhấn <strong className="text-slate-800 dark:text-slate-200">Tạo bảng mới</strong> để tạo bảng
              </li>
              <li>Nhấn vào ô số để đánh dấu khi số được xổ</li>
              <li>Nhấn lại để bỏ đánh dấu</li>
              <li>Bảng được lưu tự động</li>
            </ul>
          </div>
        )}

        {/* Generate button */}
        <div className="flex justify-center mb-8">
          <button
            onClick={handleGenerate}
            className="px-8 py-3 rounded-full font-semibold text-white
                       bg-gradient-to-r from-indigo-500 to-purple-500
                       hover:from-indigo-600 hover:to-purple-600
                       active:scale-95 transition-all shadow-lg shadow-indigo-500/25"
          >
            Tạo bảng mới
          </button>
        </div>

        {/* Grid with toast overlay */}
        {grid ? (
          <div className="relative">
            <div className="rounded-2xl overflow-hidden shadow-xl shadow-slate-200/50 dark:shadow-black/30 border border-slate-200 dark:border-slate-700">
              <div className="loto-grid">
                {grid.flat().map((num, idx) => {
                  const row = Math.floor(idx / 9);
                  const col = idx % 9;
                  const hasNumber = num > 0;
                  const isCrossed = hasNumber && crossed[row]?.[col];
                  const rowComplete =
                    hasNumber && isRowComplete(grid, crossed, row);

                  return (
                    <div
                      key={idx}
                      onClick={hasNumber ? () => handleCellClick(row, col) : undefined}
                      className={`
                        relative flex items-center justify-center
                        aspect-square text-base sm:text-xl font-bold
                        border-r border-b border-slate-200/80 dark:border-slate-700/60
                        transition-all select-none
                        ${hasNumber
                          ? isCrossed
                            ? rowComplete
                              ? "cell-crossed bg-emerald-100 dark:bg-emerald-900/40 text-emerald-500 dark:text-emerald-400 cursor-pointer"
                              : "cell-crossed bg-red-50 dark:bg-red-950/30 text-red-400 dark:text-red-500 cursor-pointer"
                            : "bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 cursor-pointer hover:bg-indigo-50 dark:hover:bg-indigo-950/30 hover:text-indigo-600 dark:hover:text-indigo-400"
                          : "bg-slate-50 dark:bg-slate-900/60"
                        }
                      `}
                    >
                      {hasNumber ? num : ""}
                    </div>
                  );
                })}
              </div>
            </div>

            {/* "Chờ X" toast overlay */}
            {toast && (
              <div
                onClick={dismissToast}
                className="absolute inset-0 flex items-center justify-center pointer-events-auto cursor-pointer z-10"
              >
                <div className="px-6 py-3 rounded-2xl bg-amber-500/90 dark:bg-amber-600/90 text-white text-xl sm:text-2xl font-black shadow-xl animate-toast">
                  {toast}
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="text-center text-slate-400 dark:text-slate-500 py-20 text-sm">
            Nhấn &ldquo;Tạo bảng mới&rdquo; để bắt đầu chơi
          </div>
        )}

        {/* Footer */}
        <footer className="mt-10 text-center text-xs text-slate-400 dark:text-slate-600">
          Made with ❤️ by{" "}
          <a
            href="https://miti99.com"
            target="_blank"
            rel="noopener noreferrer"
            className="text-indigo-500 hover:underline"
          >
            miti99
          </a>
        </footer>
      </div>

      {/* Congratulations popup */}
      {showCongrats && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm animate-fade-in"
          onClick={() => setShowCongrats(false)}
        >
          <div
            className="relative mx-4 max-w-sm w-full rounded-3xl bg-white dark:bg-slate-800 p-8 text-center shadow-2xl animate-pop-in"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="absolute -top-6 left-1/2 -translate-x-1/2 text-6xl animate-bounce-slow">
              🎉
            </div>
            <div className="absolute top-2 left-4 text-2xl animate-spin-slow">✨</div>
            <div className="absolute top-2 right-4 text-2xl animate-spin-slow-reverse">🎊</div>

            <h2 className="mt-6 text-3xl font-black bg-gradient-to-r from-amber-500 via-pink-500 to-purple-500 bg-clip-text text-transparent">
              Kinh!
            </h2>
            <p className="mt-3 text-lg text-slate-600 dark:text-slate-300">
              Hàng <span className="font-bold text-pink-500">{congratsRow}</span> đã đầy đủ!
            </p>
            <p className="mt-1 text-sm text-slate-400 dark:text-slate-500">
              Hãy hô to &ldquo;Kinh!&rdquo; 🎶
            </p>
            <button
              onClick={() => setShowCongrats(false)}
              className="mt-6 px-8 py-2.5 rounded-full font-semibold text-white
                         bg-gradient-to-r from-pink-500 to-purple-500
                         hover:from-pink-600 hover:to-purple-600
                         active:scale-95 transition-all shadow-lg"
            >
              Tuyệt vời! 🥳
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
