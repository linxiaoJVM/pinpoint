// const eslintConf = require('@pinpoint-fe/configs/eslintrc');

// module.exports = eslintConf;

module.exports = {
  parser: '@typescript-eslint/parser',
  extends: [
    'plugin:@typescript-eslint/recommended',
    'plugin:react-hooks/recommended',
    'plugin:prettier/recommended',
    'prettier',
  ],
  plugins: ['@typescript-eslint'],
  rules: {
    // Additional rules here
  },
};
