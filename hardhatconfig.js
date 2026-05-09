require("@nomicfoundation/hardhat-toolbox");
require("hardhat-gas-reporter");

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
  solidity: {
    version: "0.8.20",
    settings: {
      optimizer: {
        enabled: true,
        runs: 200
      }
    }
  },
  gasReporter: {
    enabled: true,
    currency: "USD",
    outputFile: "gas_report.txt",
    noColors: true,
    coinmarketcap: process.env.COINMARKETCAP_API_KEY
  },
  networks: {
    hardhat: {
      chainId: 1337
    }
  }
};