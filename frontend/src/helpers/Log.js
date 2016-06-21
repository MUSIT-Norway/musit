/* eslint-disable no-console */

class Level {
  static INFO = 2;
  static ERROR = 1;
}

class Log {
  constructor(level = Level.INFO) {
    this.level = level;
  }

  info(msg) {
    if (this.level >= Level.INFO && __DEVELOPMENT__) {
      console.log(`[${new Date().toString()}] INFO ----\n${msg}`);
    }
  }

  error(msg = '', exception = null) {
    if (this.level >= Level.ERROR && __DEVELOPMENT__) {
      console.error(`[${new Date().toString()}] INFO ----\n${msg}`, exception !== null ? exception : '');
    }
  }
}

export default Log;

export const logger = new Log();
