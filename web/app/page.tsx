"use client";

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

  // Load saved state on mount
  useEffect(() => {
    const savedGrid = loadGrid();
    if (savedGrid) {
      setGrid(savedGrid);
      const savedCrossed = loadCrossedState();
      setCrossed(
        savedCrossed ??
          savedGrid.map((row) => row.map(() => false))
      );
    }
  }, []);

  // Persist crossed state on change
  useEffect(() => {
    if (crossed.length > 0) {
      saveCrossedState(crossed);
    }
  }, [crossed]);

  const handleGenerate = useCallback(() => {
    if (grid && !confirm("Bạn có muốn tạo lại bảng không?")) return;
    const newGrid = generateGrid();
    setGrid(newGrid);
    const newCrossed = newGrid.map((row) => row.map(() => false));
    setCrossed(newCrossed);
    saveGrid(newGrid);
    saveCrossedState(newCrossed);
  }, [grid]);

  const handleCellClick = useCallback(
    (row: number, col: number) => {
      setCrossed((prev) => {
        const next = prev.map((r) => [...r]);
        next[row][col] = !next[row][col];
        return next;
      });
    },
    []
  );

  return (
    <div className="flex flex-col flex-1 items-center px-4 py-6 sm:py-10">
      <div className="w-full max-w-2xl">
        {/* Header */}
        <header className="text-center mb-6">
          <h1 className="text-3xl sm:text-4xl font-bold tracking-tight mb-2">
            Lô tô
          </h1>
          <p className="text-sm sm:text-base text-stone-500 dark:text-stone-400 mb-3">
            Tạo bảng chơi lô tô, lấy cảm hứng từ những buổi họp lớp thiếu
            giấy chơi lô tô của TN1 (2014-2017)
          </p>
          <button
            onClick={() => setShowInstructions((v) => !v)}
            className="text-sm text-blue-600 dark:text-blue-400 hover:underline"
          >
            {showInstructions ? "Ẩn hướng dẫn" : "Hướng dẫn"}
          </button>
        </header>

        {/* Instructions */}
        {showInstructions && (
          <div className="mb-6 rounded-lg border border-stone-200 dark:border-stone-700 bg-stone-50 dark:bg-stone-900 p-4 text-sm">
            <h2 className="font-semibold mb-2">Hướng dẫn</h2>
            <ul className="list-disc list-inside space-y-1 text-stone-600 dark:text-stone-400">
              <li>
                Nhấn <strong>Tạo bảng mới</strong> để tạo bảng mới
              </li>
              <li>Nhấn vào ô số để đánh dấu khi số được xổ</li>
              <li>Nhấn lại ô đã đánh dấu để bỏ đánh dấu</li>
              <li>Bảng và trạng thái được lưu tự động</li>
            </ul>
          </div>
        )}

        {/* Generate button */}
        <div className="flex justify-center mb-6">
          <button
            onClick={handleGenerate}
            className="px-6 py-2.5 rounded-lg bg-blue-600 text-white font-medium
                       hover:bg-blue-700 active:bg-blue-800
                       transition-colors shadow-sm"
          >
            Tạo bảng mới
          </button>
        </div>

        {/* Grid */}
        {grid && (
          <div className="overflow-x-auto rounded-xl border border-stone-200 dark:border-stone-700 shadow-sm">
            <table className="w-full border-collapse">
              <tbody>
                {grid.map((row, i) => (
                  <tr key={i}>
                    {row.map((num, j) => {
                      const hasNumber = num > 0;
                      const isCrossed =
                        hasNumber && crossed[i]?.[j];

                      return (
                        <td
                          key={j}
                          onClick={
                            hasNumber
                              ? () => handleCellClick(i, j)
                              : undefined
                          }
                          className={`
                            relative text-center border border-stone-200 dark:border-stone-700
                            h-10 sm:h-12 text-sm sm:text-lg font-medium
                            transition-colors select-none
                            ${
                              hasNumber
                                ? isCrossed
                                  ? "cell-crossed bg-amber-100 dark:bg-amber-900/40 text-amber-800 dark:text-amber-300 cursor-pointer"
                                  : "bg-amber-50 dark:bg-amber-950/30 text-stone-800 dark:text-stone-200 cursor-pointer hover:bg-amber-100 dark:hover:bg-amber-900/30"
                                : "bg-stone-50 dark:bg-stone-900/50"
                            }
                          `}
                        >
                          {hasNumber ? num : ""}
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Empty state */}
        {!grid && (
          <div className="text-center text-stone-400 dark:text-stone-500 py-16">
            Nhấn &ldquo;Tạo bảng mới&rdquo; để bắt đầu chơi
          </div>
        )}

        {/* Footer */}
        <footer className="mt-8 text-center text-xs text-stone-400 dark:text-stone-500">
          made by miti99
        </footer>
      </div>
    </div>
  );
}
