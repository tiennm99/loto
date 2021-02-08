let numInCol = [
  [1, 2, 3, 4, 5, 6, 7, 8, 9],
  [10, 11, 12, 13, 14, 15, 16, 17, 18, 19],
  [20, 21, 22, 23, 24, 25, 26, 27, 28, 29],
  [30, 31, 32, 33, 34, 35, 36, 37, 38, 39],
  [40, 41, 42, 43, 44, 45, 46, 47, 48, 49],
  [50, 51, 52, 53, 54, 55, 56, 57, 58, 59],
  [60, 61, 62, 63, 64, 65, 66, 67, 68, 69],
  [70, 71, 72, 73, 74, 75, 76, 77, 78, 79],
  [80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90]
];

let numRows = 9;
let numCols = 9;
let numPerRow = 5;
let maxNumPerCol = 6;
let lotoKey = "loto_";

function randomANumberInRow(weights) {
  let tempWeight = [...weights];
  for (let i = 1; i < tempWeight.length; i++) {
    tempWeight[i] += tempWeight[i - 1];
  }
  let rand = Math.floor(Math.random() * tempWeight[tempWeight.length - 1]);
  for (let i = 0; i < tempWeight.length; i++) {
    if (rand < tempWeight[i]) return i;
  }
}

function randomARow(baseWeight) {
  console.log("Before", JSON.stringify(baseWeight));
  let tempWeight = [...baseWeight];
  let selectedNum = [];
  for (let i = 0; i < numPerRow; i++) {
    let num = randomANumberInRow(tempWeight);
    selectedNum.push(num);
    tempWeight[num] = 0;
    baseWeight[num]--;
  }
  console.log("After", JSON.stringify(baseWeight));
  return selectedNum;
}

function randomNumbersInCol(num, col) {
  if (col < 0 || col >= numInCol.length) return;
  let arr = numInCol[col];
  arr.sort(() => 0.5 - Math.random());
  return arr.slice(0, num);
}

// function randomSelectCols() {
//   let arr = Array.from({
//     length: 9
//   }, (_, i) => i);
//   arr.sort(() => 0.5 - Math.random());
//   return arr;
// }

function generate() {
  let node = document.getElementById("grid");
  if (node.innerHTML && !confirm("Bạn có muốn tạo lại bảng không?"))
    return;
  
  let cell = new Array(numRows).fill(0).map(() => new Array(numCols).fill(0));
  let countNumPerCol = new Array(numCols).fill(0);
  // //Random cac cot co so trong tung dong
  // for (let i = 0; i < numRows; i++) {
  //   let selectedCol = randomSelectCols(numPerRow, 0, 9);
  //   let count = 0;
  //   for (let j = 0; j < selectedCol.length; j++) {
  //     let col = selectedCol[j];
  //     if (countNumPerCol[col] == maxNumPerCol) continue;
  //     countNumPerCol[col] += 1;
  //     cell[i][col] = -1;
  //     count++;
  //     if (count == numPerRow) break;
  //   }
  // }
  let baseWeight = new Array(numCols).fill(6);
  
  for (let i = 0; i < numRows; i++) {
    let newRow = randomARow(baseWeight);
    newRow.forEach(col => {
      countNumPerCol[col]++;
      cell[i][col] = -1;
    });
  }
  
  for (let i = 0; i < numCols; i++) {
    let selectedNum = randomNumbersInCol(countNumPerCol[i], i);
    for (let j = 0; j < numRows; j++) {
      if (cell[j][i] == -1) {
        cell[j][i] = selectedNum.shift();
      }
    }
  }
  
  let html = '<table border="0">';
  for (let i = 0; i < numRows; i++) {
    html += "<tr>";
    for (let j = 0; j < numCols; j++) {
      let num = cell[i][j];
      let isEnabled = num > 0;
      html += `<td id="${i}_${j}" class="${isEnabled ? 'hightlight' : ''}" onClick="cellClicked('${i}_${j}')" style="text-align: center; pointer-events: ${isEnabled ? 'auto' : 'none'};">${isEnabled ? num : ""}</td>`;
    }
    html += "</tr >";
  }
  html += "</table>";
  node.innerHTML = html;
  save(lotoKey, html);
  saveGameState();
}

function supports_html5_storage() {
  try {
    return "localStorage" in window && window["localStorage"] !== null;
  } catch (e) {
    return false;
  }
}

function cellClicked(id) {
  let elem = document.getElementById(id);
  let isChecked = elem.classList.contains("crossed");
  if (isChecked)
    elem.classList.remove("crossed");
  else
    elem.classList.add("crossed");
  saveGameState();
}

function saveGameState() {
  for (let i = 0; i < numRows; i++) {
    for (let j = 0; j < numCols; j++) {
      let id = i + "_" + j;
      let elem = document.getElementById(id);
      if (elem.innerHTML == "") continue;
      let isChecked = elem.classList.contains("crossed");
      save(lotoKey + id, !isChecked);
    }
  }
}

function loadGameState() {
  for (let i = 0; i < numRows; i++) {
    for (let j = 0; j < numCols; j++) {
      let id = i + "_" + j;
      let elem = document.getElementById(id);
      if (elem.innerHTML == "") continue;
      let isChecked = load(lotoKey + id) == "true";
      if (isChecked)
        elem.classList.remove("crossed");
      else
        elem.classList.add("crossed");
    }
  }
}

function toggle(id) {
  let elem = document.getElementById(id);
  if (elem.style.display == "block")
    elem.style.display = "none";
  else
    elem.style.display = "block";
}

save = function (key, value) {};
load = function (key) {
  return null;
};

function start() {
  if (supports_html5_storage()) {
    save = function (key, value) {
      localStorage.setItem(key, value);
    };
    load = function (key) {
      return localStorage.getItem(key);
    };
  } else {
    alert("Trình duyệt của bạn không hỗ trợ localStorage!");
    return;
  }
  
  let grid = load(lotoKey);
  if (grid) {
    console.log("Load bảng đã được tạo sẵn");
    let node = document.getElementById("grid");
    node.innerHTML = grid;
    loadGameState();
  }
}
