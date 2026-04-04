"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import {
  generateGrid,
  loadCrossedState,
  loadGrid,
  saveCrossedState,
  saveGrid,
} from "./loto-game-logic";

export default function Home() {
  const [grid, setGrid] = useState<number[][] | null>(null);
  const [crossed, setCrossed] = useState<boolean[][]>([]);
  const [showInstructions, setShowInstructions] = useState(false);

  useEffect(() => {
    const savedGrid = loadGrid();
    if (savedGrid) {
      setGrid(savedGrid);
      setCrossed(
        loadCrossedState() ?? savedGrid.map((row) => row.map(() => false))
      );
    }
  }, []);

  useEffect(() => {
    if (crossed.length > 0) saveCrossedState(crossed);
  }, [crossed]);

  const handleGenerate = useCallback(() => {
    if (grid && !confirm("Bạn có muốn tạo lại bảng không?")) return;
    const newGrid = generateGrid();
    const newCrossed = newGrid.map((row) => row.map(() => false));
    setGrid(newGrid);
    setCrossed(newCrossed);
    saveGrid(newGrid);
    saveCrossedState(newCrossed);
  }, [grid]);

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

        {/* Grid */}
        {grid ? (
          <div className="rounded-2xl overflow-hidden shadow-xl shadow-slate-200/50 dark:shadow-black/30 border border-slate-200 dark:border-slate-700">
            <div className="loto-grid">
              {grid.flat().map((num, idx) => {
                const row = Math.floor(idx / 9);
                const col = idx % 9;
                const hasNumber = num > 0;
                const isCrossed = hasNumber && crossed[row]?.[col];

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
                          ? "cell-crossed bg-red-50 dark:bg-red-950/30 text-red-400 dark:text-red-500 cursor-pointer"
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
    </div>
  );
}
