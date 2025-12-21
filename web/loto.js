function randomNumbers(num, from, to) {
  let arr = Array.from({
    length: to - from
  }, (_, i) => from + i);
  arr.sort(() => 0.5 - Math.random());
  return arr.slice(0, num);
}

let arr = randomNumbers(90, 1, 91);
let i = 0;

const readline = require('readline');
readline.emitKeypressEvents(process.stdin);
process.stdin.setRawMode(true);
process.stdin.on('keypress', (str, key) => {
  if (key.ctrl && key.name === 'c') {
    console.log(JSON.stringify);
    process.exit();
  } else {
    console.log(`Next number: ${arr[i++]}\n`);
  }
});
console.log('Press any key...');
